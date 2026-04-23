package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.sandy.scratchpad.jee.qclassifier.FileHelper.*;

@Slf4j
public class QImgClassifier {
    
    public static final File Q_BANK_DIR = new File( "/Users/sandeep/Documents/StudyNotes/question-bank" ) ;
//    public static final String GEMMA_SERVER_URL = "http://localhost:11434/api/generate" ;
    public static final String GEMMA_SERVER_URL = "http://192.168.0.149:11434/api/generate" ;
    //public static final String GEMMA_MODEL = "gemma4:26b" ;
    public static final String GEMMA_MODEL = "gemma4:latest" ;
    
    public static void main( String[] args ) {
        QImgClassifier app = new QImgClassifier() ;
        app.runClassification() ;
    }
    
    private static final int MAX_PARALLEL_THREADS = 3 ;

    private static final String[] SRC_NAME = { "AITS-15-M-PT3", "AITS-15-M-PT4", "AITS-16-M-FT1" };
    // , "AITS-16-M-FT1", "AITS-16-M-FT2", "AITS-16-M-HT5", "AITS-16-M-HT6"
    private static final String   Q_ID     = "" ;
    
    private TopicRepo topicRepo = TopicRepo.instance() ;
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable( SerializationFeature.INDENT_OUTPUT ) ;
    
    public QImgClassifier() {
    }
    
    private void runClassification() {
        log.debug( "Running classification" ) ;
        ExecutorService executor = Executors.newFixedThreadPool( MAX_PARALLEL_THREADS ) ;
        for( String srcName : SRC_NAME ) {
            executor.submit( () -> {
                log.debug( "Processing source - " + srcName ) ;
                log.debug( "----------------------------------------" ) ;
                runClassification( srcName, Q_ID ) ;
            } ) ;
        }
        executor.shutdown() ;
        try {
            executor.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS ) ;
        }
        catch( InterruptedException e ) {
            log.error( "Classification interrupted", e ) ;
            Thread.currentThread().interrupt() ;
        }
    }
    
    private void runClassification( String srcName, String qIdHint ) {
        GemmaInvoker gemmaInvoker = new GemmaInvoker() ;
        File srcDir = findDir( Q_BANK_DIR, srcName ) ;
        if( srcDir == null ) {
            log.error( "Source directory {} not found", srcName ) ;
            return ;
        }
        File[] qImgFiles = getQuestionImgFiles( srcDir, qIdHint ) ;
        if( qImgFiles == null ) {
            log.error( "No question images found for source {} and qIdHint {}", srcName, qIdHint ) ;
            return ;
        }
        ObjectNode existingMap = loadWorkspaceMapFile( srcDir ) ;
        for( File qImgFile : qImgFiles ) {
            String questionId = extractQuestionId( qImgFile ) ;
            if( existingMap.has( questionId ) ) {
//                log.debug( "Skipping {} - already mapped", questionId ) ;
                continue ;
            }
            log.debug( "Question image - {} : {}", srcDir.getName(), questionId ) ;
            long startTime = System.currentTimeMillis() ;
            List<Topic> matchedTopics = gemmaInvoker.invoke( qImgFile ) ;
            long elapsed = System.currentTimeMillis() - startTime ;
            log.debug( "\t  Classification took {}.{}s", elapsed / 1000, elapsed % 1000 ) ;
            if( matchedTopics != null ) {
                for( Topic t : matchedTopics ) {
                    log.debug( "\t  Match - {}({}) - {}", t.getTopicName(), t.getId(), t.getMatchProbability() ) ;
                }
                updateWorkspaceMapFile( srcDir, existingMap, questionId, matchedTopics ) ;
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
                    Topic cachedTopic = TopicRepo.instance().getTopicById( t.getId() );
                    
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
            log.debug( "\t  Updated map file for questionId {}", questionId ) ;
        }
        catch( Exception e ) {
            log.error( "Error updating workspace map file", e ) ;
        }
    }
}
