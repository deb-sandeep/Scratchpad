package com.sandy.scratchpad.jn.textextract;

import java.io.File ;
import java.util.*;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.util.CommandLineExec ;
import com.sandy.common.util.StringUtil ;

public class JNTextExtractor {

    private static final Logger log = Logger.getLogger( JNTextExtractor.class ) ;
    
    public static File JN_DIR = new File( 
            "/Users/sandeep/Documents/StudyNotes/JoveNotes-Std-X/Class-X" ) ;
    
    //TODO: Do the english workbook for 9th - new chapters for 10th
    public static String[] ELIGIBLE_SUBJECTS = { "Chemistry" } ;
    public static String BOOK_NAME = "Selena" ;
    
    public static void main( String[] args ) throws Exception {
        new JNTextExtractor().process() ;
    }
    
    private void process() throws Exception {
        
        File[] subjectDirs = JN_DIR.listFiles() ;
        assert subjectDirs != null;
        for( File dir : subjectDirs ) {
            if( dir.isDirectory() ) {
                
                String dirName = dir.getName() ;
                
                for( String eligibleSub : ELIGIBLE_SUBJECTS ) {
                    if( dirName.equals( eligibleSub ) ) {
                        log.debug( "Processing subject - " + dirName ) ;
                        processSubjectDir( dir ) ;
                    }
                }
            }
        }
    }
    
    private void processSubjectDir( File subDir ) throws Exception {
        
        String lang = "eng" ;
        
        if( subDir.getName().equals( "Hindi" ) ) {
            lang = "hin" ;
        }
        
        File[] chapterDirs = subDir.listFiles() ;
        assert chapterDirs != null;
        Arrays.sort( chapterDirs, ( f1, f2 ) -> {
            String f1Prefix = getChapterDirPrefix( f1 ) ;
            String f2Prefix = getChapterDirPrefix( f2 ) ;
            if( f1Prefix.equals( f2Prefix ) ) {
                return getFileSequence( f1 ) - getFileSequence( f2 ) ;
            }
            else {
                return f1Prefix.compareTo( f2Prefix ) ;
            }
        } );
        
        for( File dir : chapterDirs ) {
            if( dir.isDirectory() ) {
                log.debug( "  Processing chapter " + dir.getName() ) ;
                processChapterDir( dir, lang ) ;
            }
        }
    }
    
    private void processChapterDir( File dir, String lang ) throws Exception {
        
        if( !isValidChapterDir( dir ) ) {
            log.debug( "   Invalid chapter. Skipping" ) ;
            return ;
        }
        
        File ocrFile = new File( dir, getOCRTextRelFilePath() ) ;
        if( ocrFile.exists() ) {
            log.debug( "   OCR already exists. Skipping" ) ;
            return ;
        }

        List<File> imgFiles = getImgFiles( dir ) ;
        StringBuilder sb = new StringBuilder() ;
        for( File file : imgFiles ) {
            log.debug( "    " + file.getName() ) ;
            collectImgText( file, lang, sb ) ;
        }
        
        FileUtils.write( ocrFile, sb.toString(), "UTF-8", true ) ;
    }
    
    private String getPageImagesRelPath() {
        
        String pagesDir = "img/pages" ;
        if( BOOK_NAME != null ) {
            pagesDir = "img/books/" + BOOK_NAME + "/pages" ;
        }
        return pagesDir ;
    }
    
    private String getOCRTextRelFilePath() {
        
        String ocrFileName = "doc/ocr.txt" ;
        if( BOOK_NAME != null ) {
            ocrFileName = "doc/ocr-" + BOOK_NAME + ".txt" ;
        }
        return ocrFileName ;
    }
    
    private boolean isValidChapterDir( File dir ) {
        
        File hiResImgFolder = new File( dir, getPageImagesRelPath() ) ;
        return hiResImgFolder.exists() ;
    }
    
    private List<File> getImgFiles( File chapterDir ) {
        File imgDir = new File( chapterDir, getPageImagesRelPath() ) ;
        File[] imgFiles = imgDir.listFiles( ( dir, name ) -> name.endsWith( ".png" ) ) ;
        
        List<File> retVal = new ArrayList<>() ;
        assert imgFiles != null;
        Collections.addAll( retVal, imgFiles );
        
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
        
        return retVal ;
    }
    
    private String getFilePrefix( File file ) {
        
        String fileName = file.getName() ;
        fileName = fileName.substring( 0, fileName.length()-4 ) ;
        
        String[] parts = fileName.split( "_" ) ;
        return parts[0] ;
    }
    
    private String getChapterDirPrefix( File file ) {
        
        String fileName = file.getName() ;
        fileName = fileName.substring( 0, fileName.length()-4 ) ;
        
        String[] parts = fileName.split( "-" ) ;
        return parts[0].trim() ;
    }
    
    private int getFileSequence( File file ) {
        
        String fileName = file.getName() ;
        fileName = fileName.substring( 0, fileName.length()-4 ) ;
        
        String[] parts = fileName.split( "_" ) ;
        
        return Integer.parseInt( parts[1] );
    }
    
    private void collectImgText( File imgFile, String lang, StringBuilder sb ) {
        
        String[] cmdArgs = generateCmdArgs( imgFile, lang ) ;
        List<String> text = new ArrayList<>() ;
        
        CommandLineExec.executeCommand( cmdArgs, text ) ;
        
        boolean paragraphAdded = false ;
        
        sb.append( "\n\n------- " ).append( imgFile.getName() ).append( "\n\n" );
        for( String line : text ) {
            if( !StringUtil.isEmptyOrNull( line ) ) {
                sb.append( line.trim() ).append( " " );
                paragraphAdded = false ;
            }
            else {
                if( !paragraphAdded ) {
                    sb.append( "\n\n" ) ;
                    paragraphAdded = true ;
                }
            }
        }
    }
    
    private String[] generateCmdArgs( File imgFile, String lang ) {
        List<String> args = new ArrayList<>() ;
        args.add( "/opt/homebrew/bin/tesseract" ) ;
        args.add( imgFile.getAbsolutePath() ) ;
        args.add( "stdout" ) ;
        args.add( "-l" ) ;
        args.add( lang ) ;
        return args.toArray( new String[0] ) ;
    }
}
