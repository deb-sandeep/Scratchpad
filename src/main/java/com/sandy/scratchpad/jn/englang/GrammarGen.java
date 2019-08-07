package com.sandy.scratchpad.jn.englang;

import java.io.File ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class GrammarGen {

    private static final Logger log = Logger.getLogger( GrammarGen.class ) ;
    
    private GrammarExerciseTOC toc = null ;
    
    public void driveGeneration() throws Exception {
        log.debug( "Initializing Grammar generator" ) ;
        initialize() ;
        
        List<ExerciseMetaData> relevantEx = toc.getExercises( getExerciseTopics() ) ;
        List<ExerciseParser>   exParsers  = new ArrayList<>() ;
        for( ExerciseMetaData meta : relevantEx ) {
            log.debug( "Relevant exercise : " + meta.getDescription() ) ;
            exParsers.add( new ExerciseParser( meta ) ) ;
        }
        
        createExercises( exParsers ) ;
    }
    
    private List<String> getExerciseTopics() {
        List<String> topics = new ArrayList<String>() ;
        topics.add( "subject" ) ;
        topics.add( "predicate" ) ;
        topics.add( "sentences" ) ;
//        topics.add( "indirect" ) ;
//        topics.add( "direct " ) ;
        return topics ;
    }
    
    private void createExercises( List<ExerciseParser> exParsers )
        throws Exception {
        
        StringBuilder buffer = new StringBuilder() ;
        for( ExerciseParser parser : exParsers ) {
            try {
                String questions = parser.extractQuestions() ;
                if( questions != null ) {
                    buffer.append( parser.extractQuestions() ) ;
                    buffer.append( "\n\n" ) ;
                }
            }
            catch( Exception e ) {
                log.error( "Could not process " + parser.getMeta().getUrl(), e ) ;
            }
        }
        
        FileUtils.write( new File( "/home/sandeep/temp/grammar.txt" ), 
                         buffer.toString() ) ;
        log.debug( buffer.toString() );
    }
    
    private void initialize() throws Exception {
        toc = new GrammarExerciseTOC() ;
    }
    
    public static void main( String[] args ) 
        throws Exception {
        GrammarGen gen = new GrammarGen() ;
        gen.driveGeneration() ;
    }
}
