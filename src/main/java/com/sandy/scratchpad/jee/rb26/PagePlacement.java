package com.sandy.scratchpad.jee.rb26;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;

public class PagePlacement {
    
    private static final Logger log = Logger.getLogger( PagePlacement.class ) ;
    
    // MOD: Change this if the base directory changes
    private static final File BASE_DIR = new File( "/Users/sandeep/Documents/StudyNotes/question-bank/RB-M-C" ) ;
    
    public static void main( String[] args ) throws Exception {
        log.debug( "Page placement of RB @ " + BASE_DIR ) ;
        File[] dirs = BASE_DIR.listFiles() ;
        assert dirs != null;
        for( File dir : dirs ) {
            if( dir.isDirectory() ) {
                if( isDirValid( dir ) ) {
                    log.debug( "Processing directory " + dir.getName() ) ;
                    processNew( dir ) ;
                }
            }
        }
    }
    
    private static boolean isDirValid( File dir ) {
        String dirName = dir.getName() ;
        String dirNumStr = dirName.substring( dirName.lastIndexOf( '-' ) + 1 ) ;
        int    dirNum    = Integer.parseInt( dirNumStr ) ;
        return  dirNum >= 27 ;
    }
    
    private static void processNew( File dir ) throws IOException {
        
        File pagesDir = new File( dir, "pages" ) ;
        File pagesNewDir = new File( dir, "pages-new" ) ;
        
        if( pagesDir.exists() ) {
            FileUtils.deleteDirectory( pagesDir ) ;
        }
        
        FileUtils.moveDirectory( pagesNewDir, pagesDir ); ;
    }
    
    private static void process( File dir ) throws IOException {
        
        String dirName = dir.getName() ;
        
        File pagesDir = new File( dir, "pages" ) ;
        File pagesNewDir = new File( dir, "pages-new" ) ;
        
        if( !pagesNewDir.exists() ) {
            pagesNewDir.mkdirs() ;
        }
        
        File[] unclassifiedImgFiles = pagesDir.listFiles() ;
        assert unclassifiedImgFiles != null ;
        
        for( File uncFile : unclassifiedImgFiles ) {
            if( uncFile.isFile() && uncFile.getName().endsWith( ".png" ) ) {
                
                String fileName = uncFile.getName() ;
                fileName = fileName.substring( 0, fileName.length()-4 ) ;
                fileName = fileName.substring( fileName.lastIndexOf( '-' ) + 1 ) ;
                
                // MOD: Change this if page number needs adjustment
                int pageNum = Integer.parseInt( fileName ) - 1 ;
                File pageFile = new File( pagesNewDir, String.format( dirName + "-%03d.png", pageNum ) ) ;
                
                log.debug( "    Moving " + uncFile.getName() + " to " + pageFile.getAbsolutePath() ) ;
                FileUtils.moveFile( uncFile, pageFile ) ;
                
                log.debug( "    Deleting file " + uncFile.getAbsolutePath() ) ;
                FileUtils.deleteQuietly( uncFile ) ;
            }
        }
    }
}
