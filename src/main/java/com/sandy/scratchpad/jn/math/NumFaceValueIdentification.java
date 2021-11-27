package com.sandy.scratchpad.jn.math;

public class NumFaceValueIdentification {
    
    public static String[] FACE_VALUES = {
       "HC", "TC", "C", "TL", "L", "TTh", "Th", "H", "T", "O",
       "TB", "B", "HM", "TM", "M", "HTh"  
    } ;
    
    public static int[] FACE_VALUE_INDICES = {
        9, 8, 7, 6, 5, 4, 3, 2, 1, 0,
       10, 9, 8, 7, 6, 5
    } ;
    
    private static String[] NUMBERS = {
       "17439247",
       "12345789",
       "113344557",
       "51015200"
    } ;
    
    public void generateQuestions() {
        
        int numberIndex = (int)Math.floor( Math.random() * NUMBERS.length ) ;
        String number = NUMBERS[numberIndex] ;
        
        int numDigits = number.length() ;
        
        int fvIndex = 999 ;
        while( fvIndex > numDigits ) {
            
        }
    }
    
    public static void main( String[] args ) {
        new NumFaceValueIdentification().generateQuestions() ;
    }
}
