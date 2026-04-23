package com.sandy.scratchpad.jee.rb26;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class PagePlacement {
    
    private static final Logger log = Logger.getLogger( PagePlacement.class ) ;
    
    // MOD: Change this if the base directory changes
    private static final File MATHS_DIR = new File( "/Users/sandeep/Documents/StudyNotes/question-bank/RB-PM-M" ) ;
    private static final File PHY_DIR  = new File( "/Users/sandeep/Documents/StudyNotes/question-bank/RB-PM-P" ) ;
    private static final File CHEM_DIR = new File( "/Users/sandeep/Documents/StudyNotes/question-bank/RB-PM-C" ) ;
    
    public static void main( String[] args ) throws Exception {
        processSyllabus( CHEM_DIR ) ;
        //makeChapterDirs( PHY_DIR, 21 ) ;
    }
    
    private static void makeChapterDirs( File baseDir, int numChapters ) {
        String dirName = baseDir.getName() ;
        for( int i=1; i<=numChapters; i++ ) {
            String chapterDirName = dirName + "-" + String.format( "%02d", i ) ;
            File dir = new File( baseDir, chapterDirName ) ;
            dir.mkdirs() ;
        }
    }
    
    private static void processSyllabus( File baseDir )
        throws IOException {
        log.debug( "Page placement of RB @ " + baseDir ) ;
        File[] dirs = baseDir.listFiles() ;
        assert dirs != null;
        for( File dir : dirs ) {
            if( dir.isDirectory() ) {
                if( isDirValid( dir ) ) {
                    log.debug( "Processing directory " + dir.getName() ) ;
                    moveRawImagesToPagesDir( dir ) ;
                }
            }
        }
    }
    
    private static boolean isDirValid( File dir ) {
        String dirName = dir.getName() ;
        String dirNumStr = dirName.substring( dirName.lastIndexOf( '-' ) + 1 ) ;
        int    dirNum    = Integer.parseInt( dirNumStr ) ;
        return  dirNum >= 0 ;
    }
    
    private static void moveRawImagesToPagesDir( File dir ) throws IOException {
        
        String dirName = dir.getName() ;
        
        File pagesDir = new File( dir, "pages" ) ;
        if( !pagesDir.exists() ) {
            pagesDir.mkdirs() ;
        }
        
        File questionImgsDir = new File( dir, "question-images" ) ;
        if( !questionImgsDir.exists() ) {
            questionImgsDir.mkdirs() ;
        }
        
        File[] unclassifiedImgFiles = dir.listFiles() ;
        assert unclassifiedImgFiles != null ;
        
        for( File rawImgFile : unclassifiedImgFiles ) {
            if( rawImgFile.isFile() && rawImgFile.getName().endsWith( ".png" ) ) {
                
                String fileName = rawImgFile.getName() ;
                fileName = fileName.substring( 0, fileName.length()-4 ) ;
                fileName = fileName.substring( fileName.lastIndexOf( '_' ) + 1 ) ;
                
                int pageNum = Integer.parseInt( fileName ) + 1 ;
                File pageFile = new File( pagesDir, String.format( dirName + "-%03d.png", pageNum ) ) ;
                
                log.debug( "    Moving " + rawImgFile.getName() + " to " + pageFile.getAbsolutePath() ) ;
                FileUtils.moveFile( rawImgFile, pageFile ) ;
                
                log.debug( "       Deleting file " + rawImgFile.getAbsolutePath() ) ;
                FileUtils.deleteQuietly( rawImgFile ) ;
            }
        }
    }
}
