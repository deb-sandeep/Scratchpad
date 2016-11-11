package com.sandy.scratchpad.jn.grammarparser;

import java.io.File ;
import java.util.Collection ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class GrammarLeechProcessor {

    private static Logger logger = Logger.getLogger( GrammarLeechProcessor.class ) ;
    private static String DIR = "/home/sandeep/temp/grammar_download/txt" ; 
    
    public void process() throws Exception {
        
        Collection<File> files = null ;
        
        files = FileUtils.listFiles( new File( DIR  ), new String[]{ "txt" }, false ) ;
        for( File file : files ) {
            logger.debug( "Processing file " + file.getName() ) ;
            processFile( file ) ;
        }
    }
    
    private void processFile( File file ) throws Exception {
        
        List<String> lines = FileUtils.readLines( file ) ;
        if( hasQuestions( lines ) ) {
            FileProcessor processor = new FileProcessor( file, lines ) ;
            processor.process() ;
            processor.dump() ;
        }
        else {
            logger.error( "\tDoes not have Q/A" ) ;
        }
    }
    
    private boolean hasQuestions( List<String> lines ) {
        for( String line : lines ) {
            if( line.trim().equals( "Answers" ) ) {
                return true ;
            }
        }
        return false ;
    }
    
    public static void main( String[] args ) throws Exception {
        new GrammarLeechProcessor().process() ;
    }
}
