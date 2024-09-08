package com.sandy.scratchpad.algorithms;

import static org.apache.commons.lang.StringUtils.leftPad ;
import static org.apache.commons.lang.StringUtils.rightPad ;
import static java.lang.System.out ;

public class SelectionSort extends AlgoSimBase {
    
    public static void main( String[] args ) {
        int[] array = { 10, 5, 6, 8, 3, 2 } ;
        printArray( "Initial Array", null, array, false ) ;
        selectionSort( array ) ;
        printArray( "Sorted array:", null, array, false ) ;
    }
    
    public static void selectionSort( int[] arr ) {
        
        // The outer loop:
        // Iterate through the array one less than the length of array times.
        //                           ---------------------------------
        //
        // In each iteration, we try to swap out the value at the loop
        // index with the minimum value that exists in the
        // sub-array( outer index + 1 : end of array )
        //            ---------------
        for( int oi = 0; oi < arr.length - 1; oi++ ) {
            
            printArray( null, "oi=" + oi, arr, false, oi ) ;
            
            // Variable to keep track of which index contains the smallest
            // value in this iteration. Assume that the value at the outer loop
            // index is the minimum value in this iteration.
            
            int smallestElementIndex = oi ;
            
            // We do a nested iteration of all elements in the
            // sub-array( outer index + 1 : end of array ), trying to find
            // the smallest element in this segment.
            
            for( int ii = oi + 1; ii < arr.length; ii++ ) {
                
                if( arr[ii] < arr[smallestElementIndex] ) {
                    // If we have found an element which is lesser than the
                    // current smallest value, consider this as the new smallest
                    // value and store its position.
                    
                    smallestElementIndex = ii ;
                }
                printArray( null, "  ii=" + ii, arr, false, oi, smallestElementIndex, ii ) ;
            }
            
            // Swap out the value at outer loop index with that of the
            // smallest element value in this iteration
            swap( arr, smallestElementIndex, oi ) ;
            printArray( null, "  End", arr, false, oi ) ;
        }
    }
    
    private static void swap( int[] arr, int i1, int i2 ) {
        int temp = arr[i1] ;
        arr[i1] = arr[i2] ;
        arr[i2] = temp ;
    }
}
