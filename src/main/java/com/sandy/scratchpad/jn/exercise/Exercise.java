package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.Collection ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.log4j.Logger ;

public class Exercise {

    static final Logger log = Logger.getLogger( Exercise.class ) ;
    
    private Map<String, Question> questionMap = new LinkedHashMap<String, Question>() ;

    private String name = null ;
    private String bookName = null ;
    private String chapterName = null ;
    
    public Exercise( String bookName, String chapterName, String eId ) {
        this.bookName = bookName ;
        this.chapterName = chapterName ;
        this.name = eId ;
    }
    
    public String getExId() {
        return this.name ;
    }
    
    public String getBookName() {
        return this.bookName ;
    }
    
    public String getChapterName() {
        return this.chapterName ;
    }
    
    public void buildQuestion( ImgMeta meta ) {
        
        String qId = meta.getId() ;
        String gId = meta.getGroupId() ;

        Question question = null ;
        if( gId != null ) {
            question = getQuestionGroup( gId ) ;
        }
        else {
            question = getQuestion( qId ) ;
        }
        question.buildQuestion( meta ) ;
    }
    
    public Collection<Question> getQuestions() {
        return questionMap.values() ;
    }
    
    private QuestionGroup getQuestionGroup( String gId ) {
        
        QuestionGroup group = ( QuestionGroup )questionMap.get( gId ) ;
        if( group == null ) {
            group = new QuestionGroup() ;
            questionMap.put( gId, group ) ;
        }
        return group ;
    }
    
    private Question getQuestion( String qId ) {
        
        Question q = questionMap.get( qId ) ;
        if( q == null ) {
            q = new Question() ;
            questionMap.put( qId, q ) ;
        }
        return q ;
    }
    
    public List<Question> getQuestionsForPrinting() {
        List<Question> questions = new ArrayList<Question>() ;
        
        int questionsAddedInThisIteration = 0 ;
        do {
            questionsAddedInThisIteration = 0 ;
            for( Question q : questionMap.values() ) {
                if( q instanceof QuestionGroup ) {
                    QuestionGroup group = ( QuestionGroup )q ;
                    if( !group.isProcessed() ) {
                        for( Question gq : group.getQuestions() ) {
                            if( !gq.isProcessed() ) {
                                questions.add( gq ) ;
                                gq.setProcessed( true ) ;
                                questionsAddedInThisIteration++ ;
                                break ;
                            }
                        }
                    }
                }
                else {
                    if( !q.isProcessed() ) {
                        questions.add( q ) ;
                        q.setProcessed( true ) ;
                        questionsAddedInThisIteration++ ;
                    }
                }
            }
        }
        while( questionsAddedInThisIteration > 0 ) ;
        
        return questions ;
    }
}
