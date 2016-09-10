package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.List ;

public class QuestionGroup extends Question {

    private List<ImgMeta>  headers   = new ArrayList<ImgMeta>() ;
    private List<Question> questions = new ArrayList<Question>() ;
    
    public void addHeaderPart( ImgMeta part ) {
        if( !super.isMetaSet ) {
            super.extractMeta( part ) ;
        }
        this.headers.add( part ) ;
    }
    
    public void addQuestion( Question q ) {
        q.setGroup( this ) ;
        questions.add( q ) ;
    }
    
    public List<Question> getQuestions() {
        return this.questions ;
    }
}
