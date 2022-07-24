package com.sandy.scratchpad.jn.one2many;

public class MCQOption {
    public String answer = null ;
    public boolean isCorrect = false ;
    
    public MCQOption( String ans, boolean isCorrect ) {
        this.answer = ans ;
        this.isCorrect = isCorrect ;
    }
    
    public MCQOption( String ans ) {
        this( ans, false ) ; 
    }
}
