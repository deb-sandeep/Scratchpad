package com.sandy.scratchpad.jn.wm;

import java.io.File ;
import java.util.List ;
import java.util.logging.Logger ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.WordUtils ;

import com.sun.jersey.api.client.filter.LoggingFilter ;

public class MeaningGen {

    private String inputWordFile = "/home/sandeep/temp/words.txt" ;
    
    private WordnicAdapter adapter = null ;
    
    public MeaningGen( String key ) {
        adapter = new WordnicAdapter( key ) ;
    }
    
    public void generateMeanings() throws Exception {
        List<String> words = FileUtils.readLines( new File( inputWordFile ) ) ;
        for( String word : words ) {
            List<String> meaning = adapter.getDefinitions( word ) ;
            if( !meaning.isEmpty() ) {
                printWM( word, meaning.get( 0 ) ) ;
            }
            else {
                printWM( word, "NOT FOUND" ) ;
            }
        }
    }
    
    private void printWM( String word, String meaning ) {
        StringBuilder builder = new StringBuilder() ;
        
        meaning = meaning.replace( "\"", "\\\"" ).trim() ;
        builder.append( "@wm \"" + WordUtils.capitalize( word ) + "\"\n")
               .append( WordUtils.wrap( "\"" + meaning + "\"", 80 ) ) ;
        System.out.println( builder.toString() ) ;
        System.out.println() ;
    }
    
    public static void main( String[] args ) throws Exception {
        Logger logger = Logger.getLogger( LoggingFilter.class.getName() ) ;
        logger.setLevel( java.util.logging.Level.SEVERE ) ;
        new MeaningGen( args[0] ).generateMeanings() ;
    }
}
