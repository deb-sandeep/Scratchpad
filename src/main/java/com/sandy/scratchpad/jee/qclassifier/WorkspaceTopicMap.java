package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Load / merge / save of a source's {@code .workspace/ai-topic-map.json}.
 * <p>
 * Every question's classification is merged into the file individually so a
 * single re-queued question can be persisted without holding the whole batch's
 * in-memory tree. All access for a given file is serialised through a per-file
 * lock.
 */
@Slf4j
public final class WorkspaceTopicMap {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable( SerializationFeature.INDENT_OUTPUT ) ;

    private static final Map<String,Object> FILE_LOCKS = new ConcurrentHashMap<>() ;

    private WorkspaceTopicMap() {}

    private static File mapFile( File srcDir ) {
        return new File( srcDir, ".workspace/ai-topic-map.json" ) ;
    }

    private static Object lockFor( File srcDir ) {
        return FILE_LOCKS.computeIfAbsent( mapFile( srcDir ).getAbsolutePath(), k -> new Object() ) ;
    }

    /** True if this question already has an entry in the source's map file. */
    public static boolean isClassified( File srcDir, String questionId ) {
        synchronized( lockFor( srcDir ) ) {
            return load( srcDir ).has( questionId ) ;
        }
    }

    /** Question ids that already have an entry in the source's map file. */
    public static java.util.Set<String> classifiedQuestionIds( File srcDir ) {
        synchronized( lockFor( srcDir ) ) {
            java.util.Set<String> ids = new java.util.HashSet<>() ;
            load( srcDir ).fieldNames().forEachRemaining( ids::add ) ;
            return ids ;
        }
    }

    /**
     * Question id -> summary of its stored top topic ({@code "Topic (NN%)"}, the
     * same form a fresh classification shows) for every question already in the
     * source's map file. A question stored with no topics maps to "".
     */
    public static Map<String,String> associatedSummaries( File srcDir ) {
        synchronized( lockFor( srcDir ) ) {
            Map<String,String> summaries = new java.util.HashMap<>() ;
            load( srcDir ).fields().forEachRemaining( e -> {
                JsonNode top = e.getValue().path( "topicMappings" ).path( 0 ) ;
                summaries.put( e.getKey(), top.isMissingNode() ? "" :
                        String.format( "%s (%d%%)", top.path( "topicName" ).asText(),
                                                    top.path( "confidenceLevel" ).asInt() ) ) ;
            } ) ;
            return summaries ;
        }
    }

    /**
     * Merge one question's matched topics into the source's map file.
     * Unknown topic ids are skipped; a topic-name mismatch is logged but the
     * (authoritative) master name is used.
     */
    public static void write( File srcDir, String questionId, List<Topic> matchedTopics ) {
        synchronized( lockFor( srcDir ) ) {
            try {
                File file = mapFile( srcDir ) ;
                file.getParentFile().mkdirs() ;

                ObjectNode root = load( srcDir ) ;
                TopicRepo topicRepo = TopicRepo.instance() ;

                ArrayNode topicMappings = MAPPER.createArrayNode() ;
                if( matchedTopics != null ) {
                    for( Topic t : matchedTopics ) {
                        Topic cachedTopic = topicRepo.getTopicById( t.getId() ) ;

                        if( cachedTopic == null ) {
                            log.warn( "[{}] Model returned unknown topicId {} ({}) - skipping",
                                    questionId, t.getId(), t.getTopicName() ) ;
                            continue ;
                        }

                        if( !cachedTopic.getTopicName().equals( t.getTopicName() ) ) {
                            log.warn( "[{}] Topic name mismatch for topicId {} - master {}, model {}; using master name",
                                    questionId, t.getId(), cachedTopic.getTopicName(), t.getTopicName() ) ;
                        }

                        topicMappings.addObject()
                                .put( "topicId",         t.getId() )
                                .put( "topicName",       cachedTopic.getTopicName() )
                                .put( "syllabusName",    cachedTopic.getSyllabusName() )
                                .put( "confidenceLevel", Math.round( t.getMatchProbability() * 100 ) ) ;
                    }
                }

                root.putObject( questionId ).set( "topicMappings", topicMappings ) ;
                MAPPER.writeValue( file, root ) ;
            }
            catch( Exception e ) {
                log.error( "Error updating workspace map file for {}", srcDir.getName(), e ) ;
            }
        }
    }

    private static ObjectNode load( File srcDir ) {
        File file = mapFile( srcDir ) ;
        if( file.exists() ) {
            try {
                return (ObjectNode) MAPPER.readTree( file ) ;
            }
            catch( Exception e ) {
                log.error( "Error reading workspace map file for {}, starting fresh", srcDir.getName(), e ) ;
            }
        }
        return MAPPER.createObjectNode() ;
    }
}
