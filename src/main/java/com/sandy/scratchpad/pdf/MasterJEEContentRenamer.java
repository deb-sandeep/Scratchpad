package com.sandy.scratchpad.pdf;

import org.apache.log4j.Logger;

import java.io.File;

public class MasterJEEContentRenamer {

    private static final Logger log = Logger.getLogger(MasterJEEContentRenamer.class);
    
    public static void main( String[] args ) {
        MasterJEEContentRenamer util = new MasterJEEContentRenamer() ;
        util.processSubject( "Chemistry" ) ;
    }
    
    private void processSubject( String subjectName ) {
        log.debug( "Processing subject " + subjectName ) ;
        File dir = new File( "/Users/sandeep/temp/Resonance/" + subjectName ) ;
        
        File[] files = dir.listFiles() ;
        for( File file : files ) {
            if( file.isDirectory() ) {
                processChapter( file ) ;
            }
        }
    }
    
    private void processChapter( File file ) {
        
        String chapterName = file.getName() ;
        File[] pdfFiles = file.listFiles() ;
        
        log.debug( "\tProcessing chapter " + chapterName ) ;
        for( File pdfFile : pdfFiles ) {
            if( pdfFile.isFile() && pdfFile.getName().endsWith(".pdf") ) {
                String fileName = pdfFile.getName().toUpperCase() ;
                String fileType = getFileType( fileName );
                
                if( fileType != null ) {
                    if( fileType.equals("hlp") ) {
                        log.debug( "\t\tdeleting file - " + pdfFile.getName() ) ;
                        pdfFile.delete() ;
                    }
                    else {
                        renameFile( chapterName, pdfFile, fileType ) ;
                    }
                }
                else {
                    //log.debug( "\t\tUnindentified file " + fileName ) ;
                }
            }
        }
    }
    
    private static String getFileType( String fileName ) {
        String fileType = null ;
        if( fileName.equals( "EXERCISE.PDF" ) ) {
            fileType = "exercise" ;
        }
        else if( fileName.equals( "THEORY.PDF" ) ) {
            fileType = "theory" ;
        }
        else if( fileName.equals( "SOLUTION.PDF" ) ) {
            fileType = "exercise-solution" ;
        }
        else if( fileName.equals( "APSP.PDF" ) ) {
            fileType = "apsp" ;
        }
        return fileType;
    }
    
    private void renameFile( String chapterName, File pdfFile, String type ) {
        String newFileName = chapterName + " - " + type + ".pdf" ;
        String parentDirPath = pdfFile.getParentFile().getAbsolutePath() ;
        parentDirPath = parentDirPath.replace( "Resonance", "Resonance_mod" ) ;
        File newDir = new File( parentDirPath ) ;
        newDir.mkdirs() ;
        
        File newFile = new File( newDir, newFileName ) ;
        log.debug( "\t\tMoving - " + pdfFile.getName() + " type - " + type ) ;
        pdfFile.renameTo( newFile ) ;
    }
}
