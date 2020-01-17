package com.sandy.scratchpad.aits;

import java.io.File ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.commons.io.FileUtils ;

import com.sandy.common.util.StringUtil ;

public class AnswerManager {

    private Map<String, PaperAnswers> paperAnswers = new HashMap<>() ;
    
    public void loadAnswers( File ansFile ) throws Exception {
        List<String> lines = FileUtils.readLines( ansFile ) ;
        
        String currentPaperId = null ;
        PaperAnswers currentPaperAnswer = null ;
        
        for( String line : lines ) {
            line = line.trim() ;
            if( StringUtil.isEmptyOrNull( line ) ) continue ;
            
            if( line.startsWith( "FJ" ) ) {
                currentPaperId = line ;
                currentPaperAnswer = new PaperAnswers() ;
                paperAnswers.put( currentPaperId, currentPaperAnswer ) ;
            }
            else {
                currentPaperAnswer.parseLine( line ) ;
            }
        }
    }

    public String lookupAnswer( Question question ) {
        PaperAnswers pa = paperAnswers.get( question.paperId ) ;
        if( pa == null ) {
            throw new RuntimeException( "Answers not found for paper " + question.paperId ) ;
        }
        
        Answer answer = pa.lookupAnswer( question ) ;
        if( answer == null && question.qType.equals( "MMT" ) ) {
            return null ;
        }
        else if( answer == null ) {
            throw new RuntimeException( "Answer not found for question " + question ) ;
        }
        
        if( question.subject.equals( "Phy" ) ) {
            return answer.phyAns ;
        }
        else if( question.subject.equals( "Chem" ) ) {
            return answer.chemAns ;
        }
        else if( question.subject.equals( "Math" ) ) {
            return answer.mathAns ;
        }
        throw new RuntimeException( "Answer not found for subject " + question.subject ) ;
    }
}
