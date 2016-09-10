package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.List ;

public class Question {

    protected boolean isMetaSet = false ;
    
    private boolean       isExample     = false ;
    private String        exerciseName  = null ;
    private int[]         seqParts      = null ;
    private QuestionGroup group         = null ;
    private List<ImgMeta> questionParts = new ArrayList<ImgMeta>() ;
    private List<ImgMeta> answerParts   = new ArrayList<ImgMeta>() ;
    
    public void addQuestionPart( ImgMeta part ) {
        if( !isMetaSet ) {
            extractMeta( part ) ;
        }
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
        if( !isMetaSet ) {
            extractMeta( part ) ;
        }
        this.answerParts.add( part ) ;
    }
    
    protected void extractMeta( ImgMeta part ) {
        isMetaSet = true ;
        
        isExample    = part.isExample() ;
        exerciseName = part.getExerciseName() ;
        seqParts     = part.getSequenceParts() ;
    }
    
    public boolean isExample() {
        return isExample ;
    }
    
    public boolean isExercise() {
        return !isExample() ;
    }
    
    public String getExerciseName() {
        return this.exerciseName ;
    }
    
    public int[] getSequenceParts() {
        return this.seqParts ;
    }
}
