package com.sandy.scratchpad.jn.mkpdf;

import java.io.File ;
import java.util.ArrayList ;
import java.util.Arrays;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.CommandLineExec ;

public class JNMkPDF {

    private static final Logger log = Logger.getLogger( JNMkPDF.class ) ;
    
    public static File JN_DIR = new File( 
            "/Users/sandeep/Documents/StudyNotes/JoveNotes-Std-X/Class-X" ) ;
    
    public static void main( String[] args ) throws Exception {
        new JNMkPDF().process() ;
    }
    
    private void process() {
        
        File[] subjectDirs = JN_DIR.listFiles() ;
        assert subjectDirs != null;
        for( File dir : subjectDirs ) {
            if( dir.isDirectory() ) {
                
                String dirName = dir.getName() ;
                
                log.debug( "Processing subject - " + dirName ) ;
                processSubjectDir( dir ) ;
//                if( dirName.equals( "English" ) ) {
//                }
            }
        }
    }
    
    private void processSubjectDir( File subDir ) {
        
        File[] chaptersDirs = subDir.listFiles() ;
        assert chaptersDirs != null;
        for( File dir : chaptersDirs ) {
            if( dir.isDirectory() ) {
                log.debug( "  Processing chapter " + dir.getName() ) ;
                processChapterDir( dir ) ;
            }
        }
    }
    
    private void processChapterDir( File dir ) {
        
        if( !isValidChapterDir( dir ) ) {
            log.debug( "   Invalid chapter. Skipping" ) ;
            return ;
        }
        List<File> imgFiles = getImgFiles( dir ) ;
        if( imgFiles.size() == 0 ) {
            log.debug( "   Workbook pages don't exist. Skipping" ) ;
            return ;
        }
        
        File docDir = new File( dir, "doc" ) ;
        log.debug( "   Generating PDF" ) ;
        generatePDF( docDir, imgFiles, dir.getName() ) ;
    }
    
    private boolean isValidChapterDir( File dir ) {
        File pgScanDir = new File( dir, "img/pages" ) ;
        return pgScanDir.exists() ;
    }
    
    private List<File> getImgFiles( File chapterDir ) {
        File imgDir = new File( chapterDir, "img/pages" ) ;
        File[] imgFiles = imgDir.listFiles( ( dir, name ) -> name.endsWith( ".png" ) ) ;
        
        assert imgFiles != null;
        List<File> retVal = new ArrayList<>( Arrays.asList( imgFiles ) );
        
        retVal.sort( ( f1, f2 ) -> {
            String f1Prefix = getFilePrefix( f1 ) ;
            String f2Prefix = getFilePrefix( f2 ) ;
            if( f1Prefix.equals( f2Prefix ) ) {
                return getFileSequence( f1 ) - getFileSequence( f2 ) ;
            }
            else {
                return f1Prefix.compareTo( f2Prefix ) ;
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
        
        return Integer.parseInt( parts[1] );
    }
    
    private void generatePDF( File docDir, List<File> imgFiles, String pdfName ) {
        
        String[] cmdArgs = generateCmdArgs( docDir, imgFiles, pdfName ) ;
        List<String> text = new ArrayList<>() ;
        
        CommandLineExec.executeCommand( cmdArgs, text ) ;
        for( String out : text ) {
            log.debug( out ) ;
        }
    }
        
    private String[] generateCmdArgs( File docDir, List<File> imgFiles, String pdfName ) {
        
        List<String> args = new ArrayList<>() ;
        args.add( "/opt/homebrew/bin/convert" ) ;
        
        for( File imgFile : imgFiles ) {
            args.add( imgFile.getAbsolutePath() ) ;
        }
        
        File outFile = new File( docDir, pdfName + ".pdf" ) ;
        args.add( outFile.getAbsolutePath() ) ;
        
        return args.toArray( new String[0] ) ;
    }
}
