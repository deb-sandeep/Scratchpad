package com.sandy.scratchpad.jn.englang.punctuation;

import org.apache.commons.lang.StringEscapeUtils ;

public class Line {

    private String originalLine = null ;
    private int length = 0 ;
    private String challengeLine = null ;
    private int numPunctuations = 0 ;
    
    public Line( String string ) {
        this.originalLine = string ;
        this.length = this.originalLine.length() ;
        this.numPunctuations = countPunctuations() ;
        this.challengeLine = createChallengeLine() ;
        this.originalLine = highlightOriginalString() ;
    }
    
    private int countPunctuations() {
        int count = 0 ;
        for( int i=0; i<originalLine.length(); i++ ) {
            char ch = originalLine.charAt( i ) ;
            for( char punctuation : PunctuationGen.PUNCTUATIONS ) {
                if( ch == punctuation && ch != '"' ) {
                    count++ ;
                }
            }
        }
        return count ;
    }
    
    private String createChallengeLine() {
        StringBuilder sb = new StringBuilder() ;
        
        for( int i=0; i<originalLine.length(); i++ ) {
            char ch = originalLine.charAt( i ) ;
            boolean isPunctuation = false ;
            for( char punctuation : PunctuationGen.PUNCTUATIONS ) {
                if( ch == punctuation && ch != '"' ) {
                    isPunctuation = true ;
                    break ;
                }
            }
            
            if( !isPunctuation ) {
                sb.append( ch ) ;
            }
        }
        return sb.toString() ;
    }
    
    private String highlightOriginalString() {
        
        StringBuilder sb = new StringBuilder() ;
        
        for( int i=0; i<originalLine.length(); i++ ) {
            char ch = originalLine.charAt( i ) ;
            boolean isPunctuation = false ;
            for( char punctuation : PunctuationGen.PUNCTUATIONS ) {
                if( ch == punctuation && ch != '"' ) {
                    isPunctuation = true ;
                    break ;
                }
            }
            
            if( !isPunctuation ) {
                sb.append( ch ) ;
            }
            else {
                sb.append( "{{@red " + ch + "}}" ) ;
            }
        }
        return sb.toString() ;
    }
    
    public int length() {
        return this.length ;
    }
    
    public int getNumPunctuations() {
        return this.numPunctuations ;
    }
    
    public boolean startsWithQuote() {
        return this.originalLine.charAt( 0 ) == '"' ;
    }
    
    public String getOriginalLine() {
        return this.originalLine ;
    }
    
    public String getChallengeLine() {
        return this.challengeLine ;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder() ;
        sb.append( "@qa \"Correct the sentence by adding punctuations:\n\n" )
          .append( "**" + StringEscapeUtils.escapeJava( challengeLine ) + "**\"\n" )
          .append( "\"" + StringEscapeUtils.escapeJava( originalLine ) + "\"" ) ;
        return sb.toString() ;
    }
    
    
}
