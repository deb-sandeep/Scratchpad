package com.sandy.scratchpad.jn.math;

import java.math.BigInteger ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.lang.StringUtils ;

public class NumToWordQGen {
    
    private class WordForm {
        public String intlString ;
        public String indianString ;
        public BigInteger number ;
        public int numDigits ;
    }
    
    private List<WordForm> wordForms = new ArrayList<NumToWordQGen.WordForm>() ;
    
    private void generateNumbers() {
        
        for( int i=0; i<100; i++ ) {
            int numDigits = i/20 + 4 ;
            wordForms.add( generateWordForm( numDigits ) ) ;
        }
    }
    
    private WordForm generateWordForm( int numDigits ) {
        char digits[] = new char[numDigits] ;
        for( int i=0; i<numDigits; i++ ) {
            digits[i] = (""+(int)(Math.random()*10)).charAt( 0 ) ;
        }
        
        WordForm wordForm = new WordForm() ;
        wordForm.number = new BigInteger( String.valueOf( digits ) ) ;
        wordForm.indianString = NumToWordsIndian.convert( wordForm.number.longValue() ) ;
        wordForm.intlString = NumToWordsInternational.convert( wordForm.number.longValue() ) ;
        wordForm.numDigits = digits.length ;
        
        wordForm.indianString = StringUtils.capitalise( wordForm.indianString ) ;
        wordForm.intlString   = StringUtils.capitalise( wordForm.intlString ) ;
        
        return wordForm ;
    }
    
    private void generateQuestions() {
        
        generateNumbers() ;
        
        for( WordForm wordForm : wordForms ){
            if( wordForm.numDigits > 5 ) {
                askBothQuestions( wordForm ) ;
            }
            else {
                askQuestion( wordForm ) ;
            }
        }
    }
    
    private void askBothQuestions( WordForm wf ) {
        StringBuffer buffer = new StringBuffer() ;
        buffer.append( "@qa \"Write the **Indian** form for \n##" + wf.number.toString() + "\"\n" ) ;
        buffer.append( "\"" + wf.indianString + "\"\n\n" ) ;

        System.out.println( buffer ) ;

        buffer = new StringBuffer() ;
        buffer.append( "@qa \"Write the **International** form for \n##" + wf.number.toString() + "\"\n" ) ;
        buffer.append( "\"" + wf.intlString + "\"\n\n" ) ;
        
        System.out.println( buffer ) ;
    }
    
    private void askQuestion( WordForm wf ) {
        
        StringBuffer buffer = new StringBuffer() ;
        buffer.append( "@qa \"Write the word form for \n##" + wf.number.toString() + "\"\n" ) ;
        buffer.append( "\"" + wf.indianString + "\"\n\n" ) ;

        System.out.println( buffer ) ;
    }

    public static void main( String[] args ) {
        new NumToWordQGen().generateQuestions() ;
    }
}
