package com.sandy.scratchpad.jn.englang.punctuation;

import java.util.List ;

import org.apache.log4j.Logger ;

public class PunctuationGen {
    
    public static final Logger log = Logger.getLogger( PunctuationGen.class ) ;

    public static void main( String[] args ) throws Exception {
        new PunctuationGen().generatePunctuations() ;
    }
    
    public static char PUNCTUATIONS[] = 
        { '.', '?', '!', ',', ':', ';', '-', '\'', '"' } ;
    
    private LineLoader lineLoader = new LineLoader() ;
    
    public void generatePunctuations() throws Exception {
        
        List<Line> lines = lineLoader.loadLines() ;
        int count = 0 ;
        for( Line line : lines ) {
            if( line.length() > 20 && 
                line.length() < 70 && 
                line.getNumPunctuations() > 2 ) {
                log.debug( line ) ;
                log.debug( "" ) ;
                count++ ;
            }
        }
        log.debug( "\nLines found = " + count ) ;
    }
}
