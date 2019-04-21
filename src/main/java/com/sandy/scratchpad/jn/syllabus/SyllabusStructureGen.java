package com.sandy.scratchpad.jn.syllabus;

import java.io.File ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;

public class SyllabusStructureGen {

    private File jnRoot = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes-V/Class-5" ) ;
    private File inputFile = new File( "/home/sandeep/temp/syllabus.txt" ) ;

    private Subject currentSubject = null ;
    private List<Subject> subjects = new ArrayList<>() ;
    
    public void execute() throws Exception {
        parse() ;
        for( Subject subject : subjects ) {
            subject.createFolderStructure() ;
        }
    }
    
    private void parse() throws Exception {
        
        List<String> lines = FileUtils.readLines( inputFile ) ;
        for( String line : lines ) {
            if( line.trim().equals( "" ) ) continue ;
            
            if( line.matches( "^[0-9]+\\s+.*$" ) ) {
            
                String chNum = line.substring( 0, 3 ) ;
                String chName = line.substring( 3 ).trim() ;
                
                int chapterNumber = Integer.parseInt( chNum.trim() ) ;
                
                Chapter chapter = new Chapter( this.currentSubject, chapterNumber, chName ) ;
                this.currentSubject.addChapter( chapter ) ;
            }
            else {
                Subject subject = new Subject( line.trim(), jnRoot ) ;
                subjects.add( subject ) ;
                this.currentSubject = subject ;
            }
        }
    }
    
    public static void main( String[] args ) 
        throws Exception {
        SyllabusStructureGen gen = new SyllabusStructureGen() ;
        gen.execute() ;
    }
}
