package com.sandy.scratchpad.six2tenbooks;

import java.io.File ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class BookMerge {
    
    private static final Logger log = Logger.getLogger( BookMerge.class ) ;
    
    private static File BASE_DIR = new File( "/home/sandeep/Downloads" ) ;
    private static final String BASE_SRC_FOLDER_NAME = "ICSE Scan Books" ;
    private static File DEST_DIR = new File( BASE_DIR, BASE_SRC_FOLDER_NAME ) ;
    
    private File srcDir = null ;
    
    public BookMerge( File srcDir ) {
        this.srcDir = srcDir ;
    }
    
    public void merge( File dir ) throws Exception {
        File[] files = dir.listFiles() ;
        for( File file : files ) {
            if( file.isDirectory() ) {
                merge( file ) ;
            }
            else {
                mergeFile( file ) ;
            }
        }
    }
    
    private void mergeFile( File src ) throws Exception {
        
        String absPath = src.getAbsolutePath() ;
        String relPath = absPath.substring( this.srcDir.getAbsolutePath().length()+1 ) ;
        
        File destFile = new File( DEST_DIR, relPath ) ;
        if( !destFile.exists() ) {
            destFile.getParentFile().mkdirs() ;
            log.debug( "Copying - " + src.getAbsolutePath() ) ;
            log.debug( "\tto - " + destFile.getAbsolutePath() ) ;
            FileUtils.copyFile( src, destFile ) ;
        }
        else {
            log.debug( "File exists " + destFile.getAbsolutePath() ) ;
        }
    }

    public static void main( String[] args ) throws Exception {
        for( int i=2; i<=5; i++ ) {
            File srcDir = new File( BASE_DIR, BASE_SRC_FOLDER_NAME + " " + i ) ;
            BookMerge merger = new BookMerge( srcDir ) ;
            merger.merge( srcDir ) ;
        }
    }

}
