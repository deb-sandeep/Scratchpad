package com.sandy.scratchpad.jn.syllabus;

import java.io.File ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;

import com.sandy.common.xlsutil.XLSRow ;
import com.sandy.common.xlsutil.XLSWrapper ;

public class SyllabusStructureGen {

    private File baseDir = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes-Std-7" ) ;
    private File jnRoot = new File( baseDir, "Class-7" ) ;
    private File inputFile = new File( baseDir, "Syllabus.xlsx" ) ;


    private Map<String, Subject> subjectMap = new LinkedHashMap<>() ;
    
    public void execute() throws Exception {
        parseXlsx() ;
        for( Subject subject : subjectMap.values() ) {
            subject.createFolderStructure() ;
        }
    }
    
    private void parseXlsx() throws Exception {
        
        XLSWrapper xlWrapper = new XLSWrapper( inputFile ) ;
        List<XLSRow> rows = xlWrapper.getRows( 0, 0, 2 ) ;
        
        for( XLSRow row : rows ) {
            String subjectName = row.getCellValue( 0 ) ;
            String chapterNum  = row.getCellValue( 1 ) ;
            String chapterName = row.getCellValue( 2 ) ;
            
            Subject subject = getSubject( subjectName ) ;
            Chapter chapter = new Chapter( subject, 
                                           Integer.parseInt( chapterNum ), 
                                           chapterName ) ;
            
            subject.addChapter( chapter ) ;
        }
    }
    
    private Subject getSubject( String subjectName ) {
        
        Subject subject = subjectMap.get( subjectName ) ;
        if( subject == null ) {
            subject = new Subject( subjectName, jnRoot ) ;
            subjectMap.put( subjectName, subject ) ;
        }
        return subject ;
    }
    
    public static void main( String[] args ) 
        throws Exception {
        SyllabusStructureGen gen = new SyllabusStructureGen() ;
        gen.execute() ;
    }
}
