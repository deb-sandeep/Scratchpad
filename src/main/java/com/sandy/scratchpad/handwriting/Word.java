package com.sandy.scratchpad.handwriting;

import java.util.HashMap ;
import java.util.Map ;

public class Word {
    
    public static enum ScoreType {
        ONE_PLUS,
        ONE_PLUS_O,
        ONE_PLUS_I,
        TWO_PLUS,
        THREE_PLUS,
        TWO_MINUS,
        THREE_PLUS_TWO_MINUS,
        ONE_MINUS
    } ;
    
    private static Map<ScoreType, String> CHAR_TYPE_MAP = new HashMap<Word.ScoreType, String>() ;
    static {
        CHAR_TYPE_MAP.put( ScoreType.ONE_PLUS            , "aceiosxmnruvw" ) ;
        CHAR_TYPE_MAP.put( ScoreType.ONE_PLUS_O          , "aceosx" ) ;
        CHAR_TYPE_MAP.put( ScoreType.ONE_PLUS_I          , "mnriuvw" ) ;
        CHAR_TYPE_MAP.put( ScoreType.TWO_PLUS            , "dt" ) ;
        CHAR_TYPE_MAP.put( ScoreType.THREE_PLUS          , "fhklb" ) ;
        CHAR_TYPE_MAP.put( ScoreType.TWO_MINUS           , "fgjyz" ) ;
        CHAR_TYPE_MAP.put( ScoreType.THREE_PLUS_TWO_MINUS, "f" ) ;
        CHAR_TYPE_MAP.put( ScoreType.ONE_MINUS           , "pq" ) ;
    }
    
    private String word = null ;
    private Map<ScoreType, Integer> scoreMap = new HashMap<>() ;
    
    public Word( String word ) {
        
        this.word = word ;
        
        scoreMap.put( ScoreType.ONE_PLUS            , 0 ) ;
        scoreMap.put( ScoreType.ONE_PLUS_O          , 0 ) ;
        scoreMap.put( ScoreType.ONE_PLUS_I          , 0 ) ;
        scoreMap.put( ScoreType.TWO_PLUS            , 0 ) ;
        scoreMap.put( ScoreType.THREE_PLUS          , 0 ) ;
        scoreMap.put( ScoreType.TWO_MINUS           , 0 ) ;
        scoreMap.put( ScoreType.THREE_PLUS_TWO_MINUS, 0 ) ;
        scoreMap.put( ScoreType.ONE_MINUS           , 0 ) ;

        computeScore() ;
    }
    
    public int getScore( ScoreType scoreType ) {
        return scoreMap.get( scoreType ) ;
    }
    
    public String getWord() {
        return this.word ;
    }
    
    private void computeScore() {
        
        String lcWord = this.word.toLowerCase() ;
        for( ScoreType scoreType : ScoreType.values() ) {
            String testString = CHAR_TYPE_MAP.get( scoreType ) ;
            for( char c : lcWord.toCharArray() ) {
                if( testString.indexOf( c ) != -1 ) {
                    scoreMap.put( scoreType, scoreMap.get( scoreType ) + 1 ) ;
                }
            }
        }
    }
    
    public int hashCode() {
        return this.word.hashCode() ;
    }

    @Override
    public boolean equals( Object obj ) {
        return ((Word)obj).word.equals( this.word ) ;
    }

    public String toString() {
        return this.word + scoreMap.toString() ;
    }
}
