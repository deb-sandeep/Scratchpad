package com.sandy.scratchpad.jn.poem;

import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.io.IOUtils ;

public class LineGroup {
    
    public static final float PROBABILITY_OF_BLANK = 0.7F ;
    int startLine = 0 ;
    int numLines  = 0 ;
    int blankLineIndex = -1 ;
    
    List<String> lines = new ArrayList<String>() ;
    
    private ArrayList<String> qLines  = new ArrayList<String>() ;
    private ArrayList<String> answers = new ArrayList<String>() ;
    private List<String> ineligibleWords = null;
    
    LineGroup( int start, int numLines ) throws Exception {
        this.startLine = start ;
        this.numLines  = numLines ;
        initIneligibleWordList();
    }
    
    LineGroup( int start, int numLines, int blankLineIndex ) throws Exception {
        this( start, numLines ) ;
        this.blankLineIndex = blankLineIndex ;
    }
    
    void initIneligibleWordList() throws Exception {
        ineligibleWords = IOUtils.readLines(
                PoemFIB.class.getResourceAsStream( "/ineligibleWords.txt" )  
        );
    }
    
    void extractBlanks() {
        
        if( this.blankLineIndex == -1 ) {
            for( String line : lines ) {
                StringBuffer qLine = new StringBuffer() ;
                String[] words = line.split( "\\s+" ) ;
                
                for( String word : words ) {
                    if( word.length() <= 3 ) {
                        qLine.append( word + " " ) ;
                    }
                    else {
                        if( Math.random() > PROBABILITY_OF_BLANK && 
                            isEligibleForBlank( word ) ) {
                            
                            answers.add( word ) ;
                            qLine.append( "{" + (answers.size()-1) + "} " ) ;
                        }
                        else {
                            qLine.append( word + " ") ;
                        }
                    }
                }
                qLines.add( qLine.toString() ) ;
            }
        }
        else {
            for( int i=0; i<lines.size(); i++ ) {
                String line = lines.get( i ) ;
                
                StringBuffer qLine = new StringBuffer() ;
                String[] words = line.split( "\\s+" ) ;
                
                for( String word : words ) {
                    if( i != blankLineIndex ) {
                        qLine.append( word ).append( " " ) ;
                    }
                    else {
                        answers.add( word ) ;
                        qLine.append( "{" + (answers.size()-1) + "} " ) ;
                    }
                }
                qLines.add( qLine.toString() ) ;
            }
        }
    }
    
    boolean isEligibleForBlank( String word ) {
        if( ineligibleWords.contains( word.toLowerCase() ) ) {
            return false;
        }
        return true;
    }
    
    boolean isValid() {
        return !answers.isEmpty() ;
    }
    
    void generateFIB() {
        System.out.println( "@fib \"" ) ;
        for( String qLine : qLines ) {
            System.out.println( "### " + qLine.replaceAll( "\"", "\\\\\\\"" ) ) ;
        }
        System.out.println( "\"" ) ;
        for( String answer : answers ) {
            System.out.println( "\"" + answer.replaceAll( "\"", "\\\\\\\"" ) + "\"" ) ;
        }
    }
}
