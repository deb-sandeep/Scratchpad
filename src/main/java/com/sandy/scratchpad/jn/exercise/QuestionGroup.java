package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.Collection ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;

public class QuestionGroup extends Question {

    private Map<String, Question> questionMap = new LinkedHashMap<String, Question>() ;

    private List<ImgMeta> headers = new ArrayList<ImgMeta>() ;
    
    public void addHeaderPart( ImgMeta part ) {
        imgMeta = ( imgMeta == null ) ? part : imgMeta ;
        this.headers.add( part ) ;
    }
    
    public void buildQuestion( ImgMeta meta ) {
        if( meta.isHeader() ) {
            headers.add( meta ) ;
        }
        else {
            Question q = getQuestion( meta.getQuestionId() ) ;
            q.buildQuestion( meta ) ;
        }
    }
    
    private Question getQuestion( String qId ) {
        Question q = questionMap.get( qId ) ;
        if( q == null ) {
            q = new Question() ;
            q.setGroup( this ) ;
            questionMap.put( qId, q ) ;
        }
        return q ;
    }
    
    public Collection<Question> getQuestions() {
        return this.questionMap.values() ;
    }
    
    public List<ImgMeta> getHeaders() {
        return this.headers ;
    }
    
    public boolean isProcessed() {
        for( Question q : questionMap.values() ) {
            if( !q.processed ) {
                return false ;
            }
        }
        return true ;
    }
}
