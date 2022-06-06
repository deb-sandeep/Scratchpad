package com.sandy.scratchpad;

import java.io.File ;

public class Scratch {

    public static void main( String[] args ) {
        File file1 = new File( "/Users/sandeep/Documents/StudyNotes/JoveNotes-Std-8/Class-8/Chemistry/" ) ;
        File file2 = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes-Std-8/Class-8/Chemistry/" ) ;
        
        System.out.println( file1.equals( file2 ) ) ;
    }
}
