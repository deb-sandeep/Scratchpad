package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.Collections ;
import java.util.List ;
import java.util.regex.Matcher ;
import java.util.regex.Pattern ;

import org.apache.commons.lang.StringUtils ;
import org.apache.log4j.Logger ;

public class QuestionManager {
    
    private static Logger log = Logger.getLogger( QuestionManager.class ) ;
    private static Pattern pattern = Pattern.compile( "([0-9]+(\\.[0-9]+)*)(\\(([0-9]+)\\))?(Ans)?" ) ;
    
    private List<Question> questions = new ArrayList<Question>() ;
    
    public void buildQuestion( String imageName ) {
        Question q = new Question() ;
        if( imageName.startsWith( "ex_" ) ) {
            q.setExample( true ) ;
            parseAndPopulateExerciseImageName( imageName, q ) ;
        }
        else if( imageName.startsWith( "Ex" ) ) {
            parseAndPopulateQuestionImageName( imageName, q ) ;
        }
        
        questions.add( q ) ;
        Collections.sort( questions );
    }
    
    public List<Question> getQuestions() {
        return this.questions ;
    }
    
    private void parseAndPopulateExerciseImageName( String imgName, Question q ) {
        
        String name = StringUtils.substringAfter( imgName, "ex_" ) ;
        populateSequenceAttributes( name, q ) ;
    }
    
    private void parseAndPopulateQuestionImageName( String imgName, Question q ) {
        
        String[] nameParts = imgName.split( "_" ) ;
        String exName = StringUtils.substringAfter( nameParts[0], "Ex" ) ; 
        q.setExerciseName( exName ) ;
        
        populateSequenceAttributes( nameParts[1], q ) ;
    }
    
    private void populateSequenceAttributes( String seqPart, Question q ) {
        
        Matcher matcher = pattern.matcher( seqPart ) ;
        if( matcher.matches() ) {
            if( matcher.group( 5 ) != null ) {
                q.setAnswer( true ) ;
            }
            
            if( matcher.group( 4 ) != null ) {
                q.setPartNumber( Integer.parseInt( matcher.group( 4 ) ) ) ;
            }
            
            String[] seqParts  = matcher.group( 1 ).split( "\\." ) ;
            int[]    qSeqParts = new int[seqParts.length] ;
            
            for( int i=0; i<seqParts.length; i++ ) {
                qSeqParts[i] = Integer.parseInt( seqParts[i] ) ;
            }
            
            q.setSequenceParts( qSeqParts ) ;
        }
        else {
            throw new IllegalArgumentException( 
                 "Image " + seqPart + " does not match established pattern." ) ;
        }
    }
    
    public static void main( String[] args ) throws Exception {
        
        Matcher matcher = pattern.matcher( "1.2(23)Ans" ) ;
        
        if( matcher.matches() ) {
            for( int i=0; i<=matcher.groupCount(); i++ ) {
                log.debug( "Group " + i + " = " + matcher.group( i ) ) ;
            }
        }
    }
}
