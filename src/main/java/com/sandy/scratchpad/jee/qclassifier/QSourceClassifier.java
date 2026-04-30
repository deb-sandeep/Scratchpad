package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

import static com.sandy.scratchpad.jee.qclassifier.FileHelper.*;

@Slf4j
public class QSourceClassifier {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable( SerializationFeature.INDENT_OUTPUT ) ;

    private static final Object PROCESSED_LIST_LOCK = new Object() ;

    private final File srcDir ;
    private final String qIdHint ;
    private final GemmaInvoker gemmaInvoker ;
    private final ExecutorService executor ;
    private final File processedListFile ;
    private final TopicRepo topicRepo = TopicRepo.instance() ;

    public QSourceClassifier( File srcDir, String qIdHint, GemmaInvoker gemmaInvoker,
                              ExecutorService executor, File processedListFile ) {
        this.srcDir = srcDir ;
        this.qIdHint = qIdHint ;
        this.gemmaInvoker = gemmaInvoker ;
        this.executor = executor ;
        this.processedListFile = processedListFile ;
    }

    public void process() {
        log.debug( "Processing source - {}", srcDir.getName() ) ;
        log.debug( "----------------------------------------" ) ;

        File[] qImgFiles = getQuestionImgFiles( srcDir, qIdHint ) ;
        if( qImgFiles == null ) {
            log.error( "No question images found for source {} and qIdHint {}", srcDir.getName(), qIdHint ) ;
            return ;
        }

        ObjectNode existingMap = loadWorkspaceMapFile( srcDir ) ;
        int total = qImgFiles.length ;

        List<File> toProcess = new ArrayList<>() ;
        for( File f : qImgFiles ) {
            if( !existingMap.has( extractQuestionId( f ) ) ) toProcess.add( f ) ;
        }
        log.debug( "Source {} - {} of {} images remaining to process",
                srcDir.getName(), toProcess.size(), total ) ;

        boolean successfullyProcessed = true ;
        if( !toProcess.isEmpty() ) {
            ExecutorCompletionService<ImgResult> completion =
                    new ExecutorCompletionService<>( executor ) ;
            for( File f : toProcess ) {
                final String questionId = extractQuestionId( f ) ;
                Callable<ImgResult> task = () -> {
                    try {
                        List<Topic> topics = gemmaInvoker.invoke( f ) ;
                        return new ImgResult( questionId, topics ) ;
                    }
                    catch( Exception e ) {
                        log.error( "[{}] Error classifying image {} : {}",
                                Thread.currentThread().getName(), srcDir.getName(), questionId, e ) ;
                        return new ImgResult( questionId, null ) ;
                    }
                } ;
                completion.submit( task ) ;
            }

            int totalToProcess = toProcess.size() ;
            for( int done = 1 ; done <= totalToProcess ; done++ ) {
                try {
                    ImgResult result = completion.take().get() ;
                    log.debug( "Question image ({} of {}) - {} : {}",
                            done, totalToProcess, srcDir.getName(), result.questionId ) ;
                    if( result.matchedTopics != null ) {
                        updateWorkspaceMapFile( srcDir, existingMap, result.questionId, result.matchedTopics ) ;
                    }
                    else {
                        successfullyProcessed = false ;
                    }
                }
                catch( Exception e ) {
                    log.error( "Error awaiting image result for source {}", srcDir.getName(), e ) ;
                    successfullyProcessed = false ;
                    if( e instanceof InterruptedException ) {
                        Thread.currentThread().interrupt() ;
                        break ;
                    }
                }
            }
        }

        if( successfullyProcessed ) {
            markProcessed( srcDir.getName() ) ;
        }
    }

    private void markProcessed( String srcName ) {
        synchronized( PROCESSED_LIST_LOCK ) {
            try {
                Files.writeString( processedListFile.toPath(), srcName + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND ) ;
            }
            catch( Exception e ) {
                log.error( "Error appending to processed list file {}", processedListFile, e ) ;
            }
        }
    }

    private ObjectNode loadWorkspaceMapFile( File srcDir ) {
        File mapFile = new File( srcDir, ".workspace/ai-topic-map.json" ) ;
        if( mapFile.exists() ) {
            try {
                return (ObjectNode) MAPPER.readTree( mapFile ) ;
            }
            catch( Exception e ) {
                log.error( "Error reading workspace map file, starting fresh", e ) ;
            }
        }
        return MAPPER.createObjectNode() ;
    }

    private void updateWorkspaceMapFile( File srcDir, ObjectNode root, String questionId, List<Topic> matchedTopics ) {
        try {
            File mapFile = new File( srcDir, ".workspace/ai-topic-map.json" ) ;
            mapFile.getParentFile().mkdirs() ;

            ArrayNode topicMappings = MAPPER.createArrayNode() ;
            if( matchedTopics != null ) {
                for( Topic t : matchedTopics ) {
                    Topic cachedTopic = topicRepo.getTopicById( t.getId() );

                    if( !cachedTopic.getTopicName().equals( t.getTopicName() ) ) {
                        log.error( "Topic name mismatch for topicId {} - expected {}, found {}", t.getId(), cachedTopic.getTopicName(), t.getTopicName() ) ;
                        throw new RuntimeException( "Topic name mismatch" ) ;
                    }

                    topicMappings.addObject()
                            .put( "topicId",       t.getId() )
                            .put( "topicName",     cachedTopic.getTopicName() )
                            .put( "syllabusName",  cachedTopic.getSyllabusName() )
                            .put( "confidenceLevel", Math.round( t.getMatchProbability() * 100 ) ) ;
                }
            }

            root.putObject( questionId ).set( "topicMappings", topicMappings ) ;
            MAPPER.writeValue( mapFile, root ) ;
        }
        catch( Exception e ) {
            log.error( "Error updating workspace map file", e ) ;
        }
    }

    private static final class ImgResult {
        final String questionId ;
        final List<Topic> matchedTopics ;
        ImgResult( String questionId, List<Topic> matchedTopics ) {
            this.questionId = questionId ;
            this.matchedTopics = matchedTopics ;
        }
    }
}
