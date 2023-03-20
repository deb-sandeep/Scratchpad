package com.sandy.scratchpad.jn.mkpdf;

import java.io.File ;
import java.io.FilenameFilter ;
import java.util.ArrayList ;
import java.util.Comparator ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.CommandLineExec ;

public class JNMkPDF {

    private static final Logger log = Logger.getLogger( JNMkPDF.class ) ;
    
    public static File JN_DIR = new File( 
            "/home/sandeep/Documents/StudyNotes/JoveNotes-Std-9/Class-9" ) ;
    
    public static void main( String[] args ) throws Exception {
        new JNMkPDF().process() ;
    }
    
    private void process() throws Exception {
        
        File[] subjectDirs = JN_DIR.listFiles() ;
        for( File dir : subjectDirs ) {
            if( dir.isDirectory() ) {
                
                String dirName = dir.getName() ;
                
                if( dirName.equals( "Geography" ) ) {
                    log.debug( "Processing subject - " + dirName ) ;
                    processSubjectDir( dir ) ;
                }
            }
        }
    }
    
    private void processSubjectDir( File subDir ) throws Exception {
        
        File[] chaptersDirs = subDir.listFiles() ;
        for( File dir : chaptersDirs ) {
            if( dir.isDirectory() ) {
                log.debug( "  Processing chapter " + dir.getName() ) ;
                processChapterDir( dir ) ;
            }
        }
    }
    
    private void processChapterDir( File dir ) throws Exception {
        
        if( !isValidChapterDir( dir ) ) {
            log.debug( "   Invalid chapter. Skipping" ) ;
            return ;
        }
        List<File> imgFiles = getImgFiles( dir ) ;
        generatePDF( imgFiles, dir.getName() ) ;
    }
    
    private boolean isValidChapterDir( File dir ) {
        File pgScanDir = new File( dir, "img/pages" ) ;
        return pgScanDir.exists() ;
    }
    
    private List<File> getImgFiles( File chapterDir ) {
        File imgDir = new File( chapterDir, "img/pages" ) ;
        File[] imgFiles = imgDir.listFiles( new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return name.endsWith( ".png" ) ;
            }
        } ) ;
        
        List<File> retVal = new ArrayList<>() ;
        for( File file : imgFiles ) {
            retVal.add( file ) ;
        }
        
        retVal.sort( new Comparator<File>() {
            public int compare( File f1, File f2 ) {
                String f1Prefix = getFilePrefix( f1 ) ;
                String f2Prefix = getFilePrefix( f2 ) ;
                if( f1Prefix.equals( f2Prefix ) ) {
                    return getFileSequence( f1 ) - getFileSequence( f2 ) ;
                }
                else {
                    return f1Prefix.compareTo( f2Prefix ) ;
                }
            }
        } ) ;
        
        for( File file : retVal ) {
            log.debug( "    " + file.getName() ) ;
        }
        
        return retVal ;
    }
    
    private String getFilePrefix( File file ) {
        
        String fileName = file.getName() ;
        fileName = fileName.substring( 0, fileName.length()-4 ) ;
        
        String[] parts = fileName.split( "_" ) ;
        return parts[0] ;
    }
    
    private int getFileSequence( File file ) {
        
        String fileName = file.getName() ;
        fileName = fileName.substring( 0, fileName.length()-4 ) ;
        
        String[] parts = fileName.split( "_" ) ;
        int retVal = Integer.parseInt( parts[1] ) ;
        
        return retVal ;
    }
    
    private void generatePDF( List<File> imgFiles, String pdfName ) 
        throws Exception {
        
        String[] cmdArgs = generateCmdArgs( imgFiles, pdfName ) ;
        List<String> text = new ArrayList<>() ;
        
        CommandLineExec.executeCommand( cmdArgs, text ) ;
        for( String out : text ) {
            log.debug( out ) ;
        }
    }
        
    private String[] generateCmdArgs( List<File> imgFiles, String pdfName ) {
        
        List<String> args = new ArrayList<>() ;
        args.add( "/usr/local/bin/convert" ) ;
        
        for( File imgFile : imgFiles ) {
            args.add( imgFile.getAbsolutePath() ) ;
        }
        
        File outDir = imgFiles.get( 0 ).getParentFile() ; // img
        outDir = outDir.getParentFile() ; // pages
        outDir = outDir.getParentFile() ; // chapter
        outDir = new File( outDir, "doc" ) ;
        
        File outFile = new File( outDir, pdfName + ".pdf" ) ;
        args.add( outFile.getAbsolutePath() ) ;
        
        return args.toArray( new String[args.size()] ) ;
    }
}
