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
import java.util.List;

import static com.sandy.scratchpad.jee.qclassifier.FileHelper.*;

@Slf4j
public class QSourceClassifier implements Runnable {

    private static final ObjectMapper MAPPER =
            new ObjectMapper().enable( SerializationFeature.INDENT_OUTPUT ) ;

    private static final Object PROCESSED_LIST_LOCK = new Object() ;

    private final File srcDir ;
    private final String qIdHint ;
    private final GemmaInvoker gemmaInvoker ;
    private final File processedListFile ;
    private final TopicRepo topicRepo = TopicRepo.instance() ;

    public QSourceClassifier( File srcDir, String qIdHint, GemmaInvoker gemmaInvoker, File processedListFile ) {
        this.srcDir = srcDir ;
        this.qIdHint = qIdHint ;
        this.gemmaInvoker = gemmaInvoker ;
        this.processedListFile = processedListFile ;
    }

    @Override
    public void run() {
        log.debug( "Processing source - {}", srcDir.getName() ) ;
        log.debug( "----------------------------------------" ) ;

        File[] qImgFiles = getQuestionImgFiles( srcDir, qIdHint ) ;
        if( qImgFiles == null ) {
            log.error( "No question images found for source {} and qIdHint {}", srcDir.getName(), qIdHint ) ;
            return ;
        }

        ObjectNode existingMap = loadWorkspaceMapFile( srcDir ) ;
        int total = qImgFiles.length ;
        for( int i = 0 ; i < total ; i++ ) {
            File qImgFile = qImgFiles[i] ;
            String questionId = extractQuestionId( qImgFile ) ;
            if( existingMap.has( questionId ) ) {
                continue ;
            }

            log.debug( "Question image ({} of {}) - {} : {}", i + 1, total, srcDir.getName(), questionId ) ;
            List<Topic> matchedTopics = gemmaInvoker.invoke( qImgFile ) ;
            if( matchedTopics != null ) {
//                for( Topic t : matchedTopics ) {
//                    log.debug( "\t  Match - {} ({}) - {}",
//                    t.getTopicName(), t.getId(), t.getMatchProbability() ) ;
//                }
                updateWorkspaceMapFile( srcDir, existingMap, questionId, matchedTopics ) ;
            }
        }
        markProcessed( srcDir.getName() ) ;
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
}
