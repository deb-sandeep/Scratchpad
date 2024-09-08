package com.sandy.scratchpad.jn.refresher;

import java.io.File ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;
import com.sandy.common.xlsutil.XLSRow ;
import com.sandy.common.xlsutil.XLSWrapper ;

public class RefresherFolderStructureGen {
    
    private static final Logger log = Logger.getLogger( RefresherFolderStructureGen.class ) ;

    private File targetDir = new File( "/Users/sandeep/Documents/StudyNotes/Refreshers/Class-X" ) ;
    private File inputFile = new File( "/Users/sandeep/Documents/StudyNotes/JoveNotes-Std-X/Documents/Syllabus.xlsx" ) ;

    private Map<String, Subject> subjectMap = new LinkedHashMap<>() ;
    
    private String currentSubjectName = null ;
    
    public void execute() throws Exception {
        parseXlsx() ;
        for( Subject subject : subjectMap.values() ) {
            subject.createFolderStructure() ;
        }
    }
    
    private void parseXlsx() throws Exception {
        
        XLSWrapper xlWrapper = new XLSWrapper( inputFile ) ;
        List<XLSRow> rows = xlWrapper.getRows( 0, 0, 1 ) ;
        
        for( XLSRow row : rows ) {
            
            String col0 = row.getCellValue( 0 ) ;
            String col1 = row.getCellValue( 1 ) ;
            
            if( StringUtil.isEmptyOrNull( col0 ) ) {
                continue ;
            }
            else if( StringUtil.isEmptyOrNull( col1 ) ) {
                String subName = col0.trim() ;
                if( subName.endsWith( "-X" ) ) {
                    this.currentSubjectName = null ;
                }
                else {
                    this.currentSubjectName = col0.trim() ;
                    log.debug( "\nSubject : " + this.currentSubjectName ) ;
                    log.debug( "-------------------------------------" ) ;
                }
                continue ;
            }
            else if( this.currentSubjectName == null ) {
                continue ;
            }
            else {
                String chapterNum  = col0 ;
                String chapterName = col1 ;
                
                Subject subject = getSubject( this.currentSubjectName ) ;
                
                Chapter chapter = new Chapter( subject, 
                                               Integer.parseInt( chapterNum ), 
                                               chapterName ) ;
                
                subject.addChapter( chapter ) ;
            }
        }
    }
    
    private Subject getSubject( String subjectName ) {
        
        Subject subject = subjectMap.get( subjectName ) ;
        if( subject == null ) {
            subject = new Subject( subjectName, targetDir ) ;
            subjectMap.put( subjectName, subject ) ;
        }
        return subject ;
    }
    
    public static void main( String[] args ) 
        throws Exception {
        RefresherFolderStructureGen gen = new RefresherFolderStructureGen() ;
        gen.execute() ;
    }
}
