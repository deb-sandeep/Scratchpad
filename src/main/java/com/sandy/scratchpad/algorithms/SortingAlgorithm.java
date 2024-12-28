package com.sandy.scratchpad.algorithms;

public class SortingAlgorithm extends AlgoSimBase {
    
    public static void main( String[] args ) {
        int[] array = { 10, 5, 6, 8, 3, 2 } ;
        printArray( "Initial Array", null, array, false ) ;
        selectionSort( array ) ;
        printArray( "Sorted array:", null, array, false ) ;
    }
    
    public static void selectionSort( int[] arr ) {

        for( int oli = 0; oli < arr.length - 1; oli++ ) {
            int smallestElementIndex = oli ;
            
            for( int ili = oli + 1; ili < arr.length; ili++ ) {
                if( arr[ili] < arr[smallestElementIndex] ) {
                    smallestElementIndex = ili ;
                }
            }
            
            if( smallestElementIndex != oli ) {
                swap( arr, smallestElementIndex, oli ) ;
            }
        }
    }
    
    public static void bubbleSort( int[] arr ) {
        
        int sortedSubArrayStartIndex = arr.length ;
        
        for( int oli = 0; oli < arr.length - 1; oli++ ) {
            for( int ili = 0; ili < sortedSubArrayStartIndex-1; ili++ ) {
                if( arr[ili] > arr[ili+1] ) {
                    swap( arr, ili, ili+1 ) ;
                }
            }
            sortedSubArrayStartIndex-- ;
        }
    }
}
