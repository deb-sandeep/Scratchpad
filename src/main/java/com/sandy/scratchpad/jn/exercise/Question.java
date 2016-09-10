package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.List ;

public class Question {

    protected ImgMeta imgMeta = null ;
    
    private QuestionGroup group         = null ;
    private List<ImgMeta> questionParts = new ArrayList<ImgMeta>() ;
    private List<ImgMeta> answerParts   = new ArrayList<ImgMeta>() ;
    
    public void addQuestionPart( ImgMeta part ) {
        imgMeta = ( imgMeta == null ) ? part : imgMeta ;
        this.questionParts.add( part ) ;
    }
    
    public boolean isPartOfGroup() {
        return this.group != null ;
    }
    
    public QuestionGroup getGroup() {
        return this.group ;
    }
    
    public void setGroup( QuestionGroup group ) {
        this.group = group ;
    }
    
    public void addAnswerPart( ImgMeta part ) {
        imgMeta = ( imgMeta == null ) ? part : imgMeta ;
        this.answerParts.add( part ) ;
    }
    
    public boolean isExample() {
        return imgMeta.isExample() ;
    }
    
    public boolean isExercise() {
        return !isExample() ;
    }
    
    public String getExerciseName() {
        return imgMeta.getExerciseName() ;
    }
    
    public int[] getSequenceParts() {
        return imgMeta.getSequenceParts() ;
    }
    
    public String getId() {
        return imgMeta.getQuestionId() ;
    }
}
