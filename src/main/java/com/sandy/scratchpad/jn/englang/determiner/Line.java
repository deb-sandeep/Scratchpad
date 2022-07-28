package com.sandy.scratchpad.jn.englang.determiner;

import java.util.ArrayList ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class Line {

    private static final Logger log = Logger.getLogger( Line.class ) ;
    
    public static class Segment {
        
        public Segment( int start, int end, Determiner p ) {
            this.startIndex = start ;
            this.endIndex = end ;
            this.determiner = p ;
        }
        
        public int startIndex = 0 ;
        public int endIndex = 0 ;
        public Determiner determiner = null ;
    }
    
    private DeterminerMaster pm = DeterminerMaster.instance() ;
    
    private String lineStr = null ;
    private List<Segment> pronouns = new ArrayList<>() ;
    
    public Line( String line ) {
        this.lineStr = line ;
        extractPronouns() ;
    }
    
    public String toString() {
        return this.lineStr ;
    }
    
    public int getNumPronouns() {
        return this.pronouns.size() ;
    }
    
    public List<Segment> getPronouns() {
        return pronouns ;
    }
    
    private void extractPronouns() {
        
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
                
                checkForPronoun( startIndex, endIndex, sanitizedWord ) ;
                
                startIndex = curIndex+1 ;
            }
            curIndex++ ;
        }
    }
    
    private void checkForPronoun( int startIndex, int endIndex, String word ) {
        
        if( StringUtil.isNotEmptyOrNull( word ) ) {
            if( pm.isDeterminer( word ) ) {
                Determiner p = pm.getDeterminer( word ) ;
                pronouns.add( new Segment( startIndex, endIndex, p ) ) ;
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
        for( Segment seg : pronouns ) {
            log.debug( "    " + seg.determiner.getWord() + " (" + seg.determiner.getType().getName() + ")" );
        }
    }
}
