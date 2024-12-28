package com.sandy.scratchpad.algorithms;

import org.apache.commons.lang.StringUtils;

import java.util.Arrays;

import static java.lang.System.out;
import static org.apache.commons.lang.StringUtils.leftPad;
import static org.apache.commons.lang.StringUtils.rightPad;

public class AlgoSimBase {
    
    private static char[] MARKED_POS_BOUNDING_CHARS = { '[', ']', '{', '}', '(', ')' } ;
    
    protected static void printArray( String msg, String label, int[] arr, boolean printIndex, int... markedPositions ) {
        
        if( StringUtils .isNotBlank( msg ) ) {
            out.println( "\n" + msg ) ;
        }
        
        if( StringUtils.isBlank( label ) ) {
            label = "Values " ;
        }
        else {
            if( label.length() > 10 ) {
                label = label.substring( 0, 10 )  ;
            }
        }
        
        out.print( rightPad( label, 10 ) + ": " ) ;
        for( int i=0; i<arr.length-1; i++ ) {
            out.print( getElementPrintString( arr[i], i, markedPositions ) + "," ) ;
        }
        out.print( getElementPrintString( arr[arr.length-1], arr.length-1, markedPositions ) ) ;
        out.println() ;
        
        if( printIndex ) {
            out.print( rightPad( "Index ", 10 ) + ": " ) ;
            for( int i=0; i<arr.length-1; i++ ) {
                out.print( getElementPrintString( i, i, markedPositions ) + "," ) ;
            }
            out.print( getElementPrintString( arr.length-1, arr.length-1, markedPositions ) ) ;
        }
    }
    
    private static String getElementPrintString( int value, int index, int[] markedPositions ) {
        
        Arrays.sort( markedPositions, 0, markedPositions.length ) ;
        int markPosIndex = Arrays.binarySearch( markedPositions, index ) ;
        
        char markBoundStartChar = ' ' ;
        char markBoundEndChar = ' ' ;
        
        if( markPosIndex >= 0 ) {
            markBoundStartChar = MARKED_POS_BOUNDING_CHARS[ markPosIndex*2 ] ;
            markBoundEndChar = MARKED_POS_BOUNDING_CHARS[ markPosIndex*2 + 1 ] ;
        }
        
        StringBuilder sb = new StringBuilder() ;
        sb.append( markBoundStartChar ) ;
        sb.append( Integer.toString( value ) ) ;
        sb.append( markBoundEndChar ) ;
        
        return leftPad( sb.toString().trim(), 4 ) ;
    }
    
    protected static void swap( int[] arr, int i1, int i2 ) {
        int temp = arr[i1] ;
        arr[i1] = arr[i2] ;
        arr[i2] = temp ;
    }
}
