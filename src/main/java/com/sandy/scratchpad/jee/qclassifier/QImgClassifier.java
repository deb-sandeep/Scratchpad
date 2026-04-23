package com.sandy.scratchpad.jee.qclassifier;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.List;

import static com.sandy.scratchpad.jee.qclassifier.FileHelper.*;

@Slf4j
public class QImgClassifier {
    
    public static final File Q_BANK_DIR = new File( "/Users/sandeep/Documents/StudyNotes/question-bank" ) ;
    public static final String GEMMA_SERVER_URL = "http://192.168.0.149:11434/api/generate" ;
    //public static final String GEMMA_MODEL = "gemma4:26b" ;
    public static final String GEMMA_MODEL = "gemma4:latest" ;
    
    public static void main( String[] args ) {
        QImgClassifier app = new QImgClassifier() ;
        app.runClassification() ;
    }
    
    private static final String[] SRC_NAME = { "AITS-15-M-PT4", "AITS-16-M-FT1", "AITS-16-M-FT2", "AITS-16-M-HT5", "AITS-16-M-HT6" };
    private static final String   Q_ID     = "" ;
    
    private TopicRepo topicRepo = TopicRepo.instance() ;
    private GemmaInvoker gemmaInvoker = new GemmaInvoker() ;
    
    public QImgClassifier() {
    }
    
    private void runClassification() {
        log.debug( "Running classification" ) ;
        for( String srcName : SRC_NAME ) {
            log.debug( "Processing source - " + srcName ) ;
            log.debug( "----------------------------------------" ) ;
            runClassification( srcName, Q_ID ) ;
        }
    }
    
    private void runClassification( String srcName, String qIdHint ) {
        File srcDir = findDir( Q_BANK_DIR, srcName ) ;
        if( srcDir == null ) {
            log.error( "Source directory {} not found", SRC_NAME ) ;
        }
        else {
            File[] qImgFiles = getQuestionImgFiles( srcDir, qIdHint ) ;
            if( qImgFiles == null ) {
                log.error( "No question images found for source {} and qIdHint {}",
                           srcName, qIdHint ) ;
            }
            else {
                for( File qImgFile : qImgFiles ) {
                    log.debug( "" ) ;
                    log.debug( "Question image - " + extractQuestionId( qImgFile ) ) ;
                    List<Topic> matchedTopics = gemmaInvoker.invoke( qImgFile ) ;
                    for( Topic t : matchedTopics ) {
                        log.debug( "\t  Match - {}({}) - {}", t.getTopicName(), t.getId(), t.getMatchProbability() );
                    }
                    updateWorkspaceMapFile( srcDir, qImgFile, matchedTopics ) ;
                }
            }
        }
    }
    
    private static final ObjectMapper MAPPER = new ObjectMapper()
            .enable( SerializationFeature.INDENT_OUTPUT ) ;

    private void updateWorkspaceMapFile( File srcDir, File qImgFile, List<Topic> matchedTopics ) {
        try {
            File mapFile = new File( srcDir, ".workspace/ai-topics-map.json" ) ;

            ObjectNode root ;
            if( mapFile.exists() ) {
                root = (ObjectNode) MAPPER.readTree( mapFile ) ;
            }
            else {
                mapFile.getParentFile().mkdirs() ;
                root = MAPPER.createObjectNode() ;
            }

            String questionId = extractQuestionId( qImgFile ) ;

            ArrayNode topicMappings = MAPPER.createArrayNode() ;
            if( matchedTopics != null ) {
                for( Topic t : matchedTopics ) {
                    Topic cachedTopic = TopicRepo.instance().getTopicById( t.getId() );
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
