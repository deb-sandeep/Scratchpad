package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies QImgExtractor's auto topic association to a freshly classified
 * question, writing the chosen topic into the source's
 * {@code .workspace/question-info.json} so QImgExtractor shows it as mapped.
 * <p>
 * The quality gate is a port of QImgExtractor's {@code AutoTopicAssociator}
 * (recommendations confidence-descending, the second one's confidence taken as
 * 0 when there is only one):
 * <ul>
 *   <li>Any subject : top confidence &ge; 90 and it leads the second by 10 or more.</li>
 *   <li>Physics / Maths : otherwise top confidence &ge; 95.</li>
 *   <li>Chemistry : otherwise top confidence &ge; 95 and it leads the second by more than 5.</li>
 * </ul>
 * A question that already has a topic (manual or earlier auto association) is
 * never changed. Sources without a question-info.json (never opened in
 * QImgExtractor) are skipped.
 * <p>
 * <b>Caveat:</b> if the source is open in QImgExtractor while it is being
 * classified, QImgExtractor's next save rewrites question-info.json from its
 * in-memory model and drops these topics. Reopen the project in QImgExtractor
 * after classification.
 */
@Slf4j
public final class QuestionTopicAssociator {

    public enum Outcome {
        ASSOCIATED, ALREADY_SET, FAILED_GATE, NO_RECOMMENDATION,
        NO_QUESTION_INFO, NOT_IN_QUESTION_INFO, ERROR
    }

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable( SerializationFeature.INDENT_OUTPUT ) ;

    private static final Map<String,Object> FILE_LOCKS = new ConcurrentHashMap<>() ;

    private QuestionTopicAssociator() {}

    private static File infoFile( File srcDir ) {
        return new File( srcDir, ".workspace/question-info.json" ) ;
    }

    private static Object lockFor( File file ) {
        return FILE_LOCKS.computeIfAbsent( file.getAbsolutePath(), k -> new Object() ) ;
    }

    /** Associate the question's top stored AI topic if it clears the quality gate. */
    public static Outcome associate( File srcDir, String questionId ) {

        File file = infoFile( srcDir ) ;
        if( !file.exists() ) return result( questionId, Outcome.NO_QUESTION_INFO ) ;

        List<WorkspaceTopicMap.StoredMapping> mappings =
                WorkspaceTopicMap.topicMappings( srcDir, questionId ) ;
        if( mappings.isEmpty() ) return result( questionId, Outcome.NO_RECOMMENDATION ) ;

        Topic topic = evaluate( QTask.subjectOf( questionId ), mappings ) ;
        if( topic == null ) return result( questionId, Outcome.FAILED_GATE ) ;

        String qid = questionId.replace( '_', '/' ) ;
        synchronized( lockFor( file ) ) {
            try {
                ObjectNode root = (ObjectNode) MAPPER.readTree( file ) ;
                ObjectNode question = null ;
                for( JsonNode q : root.path( "questions" ) ) {
                    if( qid.equals( q.path( "qid" ).asText() ) ) {
                        question = (ObjectNode) q ;
                        break ;
                    }
                }
                if( question == null ) return result( questionId, Outcome.NOT_IN_QUESTION_INFO ) ;

                JsonNode existing = question.get( "topic" ) ;
                if( existing != null && !existing.isNull() ) return result( questionId, Outcome.ALREADY_SET ) ;

                question.putObject( "topic" )
                        .put( "id",           topic.getId() )
                        .put( "name",         topic.getTopicName() )
                        .put( "syllabusName", topic.getSyllabusName() ) ;

                // Write-then-rename so QImgExtractor never reads a half-written file.
                File tmp = new File( file.getParentFile(), file.getName() + ".tmp" ) ;
                MAPPER.writeValue( tmp, root ) ;
                Files.move( tmp.toPath(), file.toPath(),
                            StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE ) ;
            }
            catch( Exception e ) {
                log.error( "[{}] Error associating topic in {}", questionId, file, e ) ;
                return Outcome.ERROR ;
            }
        }
        log.info( "[{}] {} auto-associated with topic {} ({})",
                srcDir.getName(), questionId, topic.getId(), topic.getTopicName() ) ;
        return Outcome.ASSOCIATED ;
    }

    /** @return the topic to assign, or null if the quality gate is not cleared. */
    private static Topic evaluate( String subjectCode, List<WorkspaceTopicMap.StoredMapping> sorted ) {

        WorkspaceTopicMap.StoredMapping top = sorted.get( 0 ) ;
        Topic topic = TopicRepo.instance().getTopicById( top.topicId ) ;
        if( topic == null ) return null ;

        int topConfidence = top.confidenceLevel ;
        int secondConfidence = sorted.size() > 1 ? sorted.get( 1 ).confidenceLevel : 0 ;

        if( topConfidence >= 90 && ( topConfidence - secondConfidence ) >= 10 ) {
            return topic ;
        }

        return switch( subjectCode ) {
            case "P", "M" -> topConfidence >= 95 ? topic : null ;
            case "C" -> topConfidence >= 95 && ( topConfidence - secondConfidence ) > 5 ? topic : null ;
            default -> null ;
        } ;
    }

    private static Outcome result( String questionId, Outcome outcome ) {
        log.debug( "[{}] Topic association: {}", questionId, outcome ) ;
        return outcome ;
    }
}
