package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.List ;

public class Question {

    protected ImgMeta imgMeta = null ;
    protected boolean processed = false ;
    
    private QuestionGroup group         = null ;
    private List<ImgMeta> questionParts = new ArrayList<ImgMeta>() ;
    private List<ImgMeta> answerParts   = new ArrayList<ImgMeta>() ;
    
    public boolean isPartOfGroup() {
        return this.group != null ;
    }
    
    public QuestionGroup getGroup() {
        return this.group ;
    }
    
    public void setGroup( QuestionGroup group ) {
        this.group = group ;
    }
    
    public String getExerciseName() {
        return imgMeta.getExerciseName() ;
    }
    
    public String getId() {
        return imgMeta.getId() ;
    }
    
    public void buildQuestion( ImgMeta meta ) {
        if( this.imgMeta == null ) this.imgMeta = meta ;
        if( meta.isAnswer() ) {
            answerParts.add( meta ) ;
        }
        else {
            questionParts.add( meta ) ;
        }
    }
    
    public List<ImgMeta> getQuestionParts() {
        return this.questionParts ;
    }
    
    public List<ImgMeta> getAnswerParts() {
        return this.answerParts ;
    }
    
    public void setProcessed( boolean processed ) {
        this.processed = true ;
    }
    
    public boolean isProcessed() {
        return this.processed ;
    }
}
