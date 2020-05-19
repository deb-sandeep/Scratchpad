package com.sandy.scratchpad.jeeqsorter;

import org.apache.commons.lang.ArrayUtils ;

public class Question {
    
    String sub = null ;
    String paperId = null ;
    String qType = null ;
    float qId = 0.0F ;
    int qNumber = 0 ;
    
    Question( String fileName ) {
        
        String fName = fileName.substring( 0, fileName.length()-4 ) ;
        if( fName.contains( "(" ) ) {
            fName = fName.substring( 0, fName.indexOf( '(' ) ) ;
        }
        
        String[] parts = fName.split( "_" ) ;
        int qTypePartIndex = getQTypePartIndex( parts ) ;
        
        sub = parts[0] ;
        qType = parts[qTypePartIndex] ;
        paperId = String.join( "_", (String[])ArrayUtils.subarray( parts, 2, qTypePartIndex ) ) ;

        String qIdStr = "" ;
        for( int i=qTypePartIndex+1; i<parts.length; i++ ) {
            qIdStr += parts[i] ;
            if( i < parts.length-1 ) {
                qIdStr += "." ;
            }
            
            if( i == parts.length-1 ) {
                qNumber = Integer.parseInt( parts[i] ) ;
            }
        }
        qId = Float.parseFloat( qIdStr ) ;
    }
    
    public boolean isLCTParagraph() {
        return qType.equals( "LCT" ) && ( qId == (int)qId ) ;
    }
    
    private int getQTypePartIndex( String[] parts ) {
        for( int i=2; i<parts.length-1; i++ ) {
            String part = parts[i] ;
            if( part.equals( "SCA" ) || 
                part.equals( "MCA" ) || 
                part.equals( "MMT" ) || 
                part.equals( "NT" ) || 
                part.equals( "LCT" ) ) {
                return i ;
            }
        }
        throw new RuntimeException( "No qtype found in " + String.join( "_", parts ) ) ;
    }

    public static void main( String[] args ) {
        System.out.println( 2.00 > (int)2.34 ) ;
    }
}


