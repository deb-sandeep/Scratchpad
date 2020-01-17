package com.sandy.scratchpad.aits;

import java.util.HashMap ;
import java.util.Map ;

import org.apache.log4j.Logger ;

public class PaperAnswers {

    static final Logger log = Logger.getLogger( PaperAnswers.class ) ;
    
    private Map<String, Map<Integer, Answer>> answerMap = new HashMap<>() ;
    
    public void parseLine( String line ) {
        String[] parts = line.split( "\\s+" ) ;
        if( parts.length != 5 ) {
            throw new RuntimeException( "Invalid ans line. " + line  ) ;
        }
        
        Map<Integer, Answer> answersForType = null ;
        Answer answer = null ;
        
        String  qType   = parts[0].trim() ;
        Integer qNo     = Integer.parseInt( parts[1].trim() ) ;
        String  phyAns  = parts[2].trim() ;
        String  chemAns = parts[3].trim() ;
        String  mathAns = parts[4].trim() ;
        
        answersForType = ( Map<Integer, Answer> )answerMap.get( qType ) ;
        
        if( answersForType == null ) {
            answersForType = new HashMap<>() ;
            answerMap.put( qType, answersForType ) ;
        }
        
        answer = answersForType.get( qNo ) ;
        if( answer == null ) {
            answer = new Answer( phyAns, chemAns, mathAns ) ;
            answersForType.put( qNo, answer ) ;
        }
    }

    public Answer lookupAnswer( Question question ) {
        Map<Integer, Answer> answers = answerMap.get( question.qType ) ;
        if( question.qType.equals( "MMT" ) && answers == null ) {
            return null ;
        }
        if( answers == null ) {
            throw new RuntimeException( "No answers found for question type " + question ) ;
        }
        return answers.get( question.qNo ) ;
    }
}
