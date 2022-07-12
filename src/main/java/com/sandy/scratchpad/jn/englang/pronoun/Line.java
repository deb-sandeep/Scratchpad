package com.sandy.scratchpad.jn.englang.pronoun;

import org.apache.log4j.Logger ;

public class Line {

    private static final Logger log = Logger.getLogger( Line.class ) ;
    
    private String lineStr = null ;
    private PronounMaster pm = PronounMaster.instance() ;
    
    public Line( String line ) {
        this.lineStr = line ;
        extractPronouns() ;
    }
    
    private void extractPronouns() {
        
        String[] words = lineStr.split( "\\s+" ) ;
        for( String word : words ) {
            
            String sanitizedWord = sanitize( word ) ;
            
            if( pm.isPronoun( sanitizedWord ) ) {
                Pronoun p = pm.getPronoun( sanitizedWord ) ;
                log.debug( "  " + sanitizedWord + " (" + p.getType().getName() + ")" ) ;
            }
        }
    }
    
    private String sanitize( String word ) {
        
        String sanitizedWord = word ;
        
        if( !sanitizedWord.substring( 0, 1 ).matches( "[A-Za-z]" ) ) {
            sanitizedWord = sanitizedWord.substring( 1 ) ;
        }
        
        if( !sanitizedWord.substring( sanitizedWord.length()-1 ).matches( "[A-Za-z]" ) ) {
            sanitizedWord = sanitizedWord.substring( 0, sanitizedWord.length()-1 ) ;
        }
        
        return sanitizedWord ;
    }
    
    public static void main( String[] args ) {
        new Line( "Hi there himself!" ) ;
    }
}
