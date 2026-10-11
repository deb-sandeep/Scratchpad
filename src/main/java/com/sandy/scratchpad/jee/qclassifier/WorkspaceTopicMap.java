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

    /** Topic-level flag marking a mapping chosen by hand rather than by the model. */
    static final String HUMAN_INTERVENTION = "humanIntervention" ;

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
                        summary( top.path( "topicName" ).asText(), top.path( "confidenceLevel" ).asInt(),
                                 top.path( HUMAN_INTERVENTION ).asBoolean() ) ) ;
            } ) ;
            return summaries ;
        }
    }

    /** Question id -> topic id of its stored top topic, for every question stored with topics. */
    public static Map<String,Integer> topTopicIds( File srcDir ) {
        synchronized( lockFor( srcDir ) ) {
            Map<String,Integer> ids = new java.util.HashMap<>() ;
            load( srcDir ).fields().forEachRemaining( e -> {
                JsonNode top = e.getValue().path( "topicMappings" ).path( 0 ) ;
                if( !top.isMissingNode() ) ids.put( e.getKey(), top.path( "topicId" ).asInt() ) ;
            } ) ;
            return ids ;
        }
    }

    /** Row summary of a stored mapping: {@code "Topic (NN%)"}, suffixed {@code " [manual]"} if human-chosen. */
    static String summary( String topicName, int confidenceLevel, boolean humanIntervention ) {
        return String.format( "%s (%d%%)%s", topicName, confidenceLevel,
                              humanIntervention ? " [manual]" : "" ) ;
    }

    /** One stored topic recommendation, as persisted in the map file. */
    public static final class StoredMapping {
        public final int topicId ;
        public final int confidenceLevel ;

        StoredMapping( int topicId, int confidenceLevel ) {
            this.topicId = topicId ;
            this.confidenceLevel = confidenceLevel ;
        }
    }

    /**
     * The stored recommendations for one question, confidence-descending.
     * Empty if the question has no entry or was stored with no topics.
     */
    public static List<StoredMapping> topicMappings( File srcDir, String questionId ) {
        synchronized( lockFor( srcDir ) ) {
            List<StoredMapping> mappings = new java.util.ArrayList<>() ;
            load( srcDir ).path( questionId ).path( "topicMappings" ).forEach( m ->
                    mappings.add( new StoredMapping( m.path( "topicId" ).asInt(),
                                                     m.path( "confidenceLevel" ).asInt() ) ) ) ;
            mappings.sort( ( a, b ) -> b.confidenceLevel - a.confidenceLevel ) ;
            return mappings ;
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

                        addMapping( topicMappings, cachedTopic, Math.round( t.getMatchProbability() * 100 ) ) ;
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

    /**
     * Replace the question's stored recommendations with a single hand-picked
     * topic at 100% confidence, flagged {@value #HUMAN_INTERVENTION}.
     */
    public static void writeManual( File srcDir, String questionId, Topic topic ) {
        synchronized( lockFor( srcDir ) ) {
            try {
                File file = mapFile( srcDir ) ;
                file.getParentFile().mkdirs() ;

                ObjectNode root = load( srcDir ) ;
                ArrayNode topicMappings = MAPPER.createArrayNode() ;
                addMapping( topicMappings, topic, 100 ).put( HUMAN_INTERVENTION, true ) ;

                root.putObject( questionId ).set( "topicMappings", topicMappings ) ;
                MAPPER.writeValue( file, root ) ;
            }
            catch( Exception e ) {
                throw new RuntimeException( "Error updating workspace map file for " + srcDir.getName(), e ) ;
            }
        }
    }

    private static ObjectNode addMapping( ArrayNode topicMappings, Topic topic, int confidenceLevel ) {
        return topicMappings.addObject()
                .put( "topicId",         topic.getId() )
                .put( "topicName",       topic.getTopicName() )
                .put( "syllabusName",    topic.getSyllabusName() )
                .put( "confidenceLevel", confidenceLevel ) ;
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
