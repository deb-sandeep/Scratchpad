package com.sandy.scratchpad.jn.paryayvachi;

import java.util.ArrayList ;
import java.util.Collections ;
import java.util.List ;

public class MCQ {

    private String question = null ;
    private List<MCQOption> options = new ArrayList<>() ;
    
    public MCQ( String question ) {
        this.question = question ;
    }
    
    public void addOption( MCQOption option ) {
        this.options.add( option ) ;
        Collections.shuffle( this.options ) ;
    }
    
    public int getNumOptions() {
        return this.options.size() ;
    }
    
    public boolean containsOption( String optionString ) {
        for( MCQOption option : options ) {
            if( option.answer.equals( optionString ) ) 
                return true ;
        }
        return false ;
    }
    
    public String toString() {

        StringBuilder sb = new StringBuilder() ;
        sb.append( "@multi_choice \"" + question + "\" {\n" )
          .append( "    @options {\n" ) ;
        
        for( MCQOption option : options ) {
            sb.append( "       \"" + option.answer + "\"" ) ;
            if( option.isCorrect ) {
                sb.append( " correct" ) ;
            }
            sb.append( ",\n" ) ;
        }
        
        sb.deleteCharAt( sb.length()-1 ) ;
        sb.deleteCharAt( sb.length()-1 ) ;
        sb.append( "\n    }" ) ;
        sb.append( "\n    @numOptionsPerRow 3" ) ;
        sb.append( "\n}" ) ;
        return sb.toString() ;
    }
}
