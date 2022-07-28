package com.sandy.scratchpad.jn.englang.preposition;

import java.util.ArrayList ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class Line {

    private static final Logger log = Logger.getLogger( Line.class ) ;
    
    public static class PrepositionSegment {
        
        public PrepositionSegment( int start, int end, String preposition ) {
            this.startIndex = start ;
            this.endIndex = end ;
            this.preposition = preposition ;
        }
        
        public int startIndex = 0 ;
        public int endIndex = 0 ;
        public String preposition = null ;
    }
    
    private PrepositionMaster pm = PrepositionMaster.instance() ;
    
    private String lineStr = null ;
    private List<PrepositionSegment> prepositionSegments = new ArrayList<>() ;
    
    public Line( String line ) {
        this.lineStr = line ;
        extractPrepositions() ;
    }
    
    public String toString() {
        return this.lineStr ;
    }
    
    public int getNumPrepositions() {
        return this.prepositionSegments.size() ;
    }
    
    public List<PrepositionSegment> getPrepositionSegments() {
        return prepositionSegments ;
    }
    
    private void extractPrepositions() {
        
        int curIndex = 0 ;
        int startIndex = 0 ;
        
        while( curIndex < lineStr.length() ) {
            
            char ch = lineStr.charAt( curIndex ) ;
            if( ch == ' ' || curIndex == lineStr.length()-1 ) {
                
                int endIndex = curIndex ;
                if( curIndex == lineStr.length()-1 ) {
                    endIndex = lineStr.length() ;
                }
                
                String word = lineStr.substring( startIndex, endIndex ) ;
                String sanitizedWord = sanitize( word ) ;
                
                int beginIndex = word.indexOf( sanitizedWord ) ;
                startIndex += beginIndex ;
                endIndex    = startIndex + sanitizedWord.length() ;
                
                checkForPreposition( startIndex, endIndex, sanitizedWord ) ;
                
                startIndex = curIndex+1 ;
            }
            curIndex++ ;
        }
    }
    
    private void checkForPreposition( int startIndex, int endIndex, String word ) {
        
        PrepositionSegment seg = null ;
        
        if( StringUtil.isNotEmptyOrNull( word ) ) {
            if( pm.isPresposition( word ) ) {
                seg = new PrepositionSegment( startIndex, endIndex, 
                                              word.toLowerCase() ) ;
                prepositionSegments.add( seg ) ;
            }
        }
    }
    
    private String sanitize( String word ) {
        
        StringBuilder sb = new StringBuilder() ;
        for( int i=0; i<word.length(); i++ ) {
            char ch = word.charAt( i ) ;
            if( isChar( ch ) ) {
                sb.append( ch ) ;
            }
            else {
                if( i != 0 ) {
                    if( i < word.length()-1 ) {
                        char nextChar = word.charAt( i+1 ) ;
                        if( isChar( nextChar ) ) {
                            sb.append( ch ) ;
                        }
                    }
                }
            }
        }
        return sb.toString() ;
    }
    
    private boolean isChar( char ch ) {
        return ( ch >= 'a' && ch <= 'z' ) || ( ch >= 'A' && ch <= 'Z' ) ;
    }

    @Override
    public boolean equals( Object obj ) {
        return obj == this ;
    }
    
    public void print() {
        
        log.debug( ">> " + lineStr ) ;
        for( PrepositionSegment seg : prepositionSegments ) {
            log.debug( "    " + seg.preposition ) ;
        }
    }
}
