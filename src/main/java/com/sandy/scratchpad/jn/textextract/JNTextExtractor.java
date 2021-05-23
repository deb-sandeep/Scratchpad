package com.sandy.scratchpad.jn.textextract;

import java.io.File ;
import java.io.FilenameFilter ;
import java.util.ArrayList ;
import java.util.Comparator ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.util.CommandLineExec ;
import com.sandy.common.util.StringUtil ;

public class JNTextExtractor {

    private static final Logger log = Logger.getLogger( JNTextExtractor.class ) ;
    
    public static File JN_DIR = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes-Std-7/Class-7" ) ;
    
    public static void main( String[] args ) throws Exception {
        new JNTextExtractor().process() ;
    }
    
    private void process() throws Exception {
        
        File[] subjectDirs = JN_DIR.listFiles() ;
        for( File dir : subjectDirs ) {
            if( dir.isDirectory() ) {
                
                String dirName = dir.getName() ;
                
                if( dirName.equals( "Hindi" ) ) continue ;
                if( dirName.equals( "English Grammar" ) ) continue ;
                if( dirName.equals( "Mathematics" ) ) continue ;
                
                log.debug( "Processing subject - " + dirName ) ;
                processSubjectDir( dir ) ;
            }
        }
    }
    
    private void processSubjectDir( File subDir ) throws Exception {
        
        String lang = "eng" ;
        
        if( subDir.getName().equals( "Hindi" ) ) {
            lang = "hin" ;
        }
        
        File[] chaptersDirs = subDir.listFiles() ;
        for( File dir : chaptersDirs ) {
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
        
        List<File> imgFiles = getHiResImgFiles( dir ) ;
        StringBuilder sb = new StringBuilder() ;
        for( File file : imgFiles ) {
            log.debug( "    " + file.getName() ) ;
            collectImgText( file, lang, sb ) ;
        }
        
        File ocrFile = new File( dir, "doc/ocr.txt" ) ;
        FileUtils.write( ocrFile, sb.toString(), "UTF-8", true ) ;
    }
    
    private boolean isValidChapterDir( File dir ) {
        File hiResImgFolder = new File( dir, "img/pages/hi-res" ) ;
        return hiResImgFolder.exists() ;
    }
    
    private List<File> getHiResImgFiles( File chapterDir ) {
        File hiResImgDir = new File( chapterDir, "img/pages/hi-res" ) ;
        File[] imgFiles = hiResImgDir.listFiles( new FilenameFilter() {
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
                return getFileId( f1 ) - getFileId( f2 ) ;
            }
        } ) ;
        return retVal ;
    }
    
    private int getFileId( File file ) {
        String[] parts = file.getName().split( "_" ) ;
        int retVal = Integer.parseInt( parts[0] )*10 ;
        retVal += Integer.parseInt( parts[1] ) ;
        return retVal ;
    }
    
    private void collectImgText( File imgFile, String lang, StringBuilder sb ) 
        throws Exception {
        
        String[] cmdArgs = generateCmdArgs( imgFile, lang ) ;
        List<String> text = new ArrayList<>() ;
        
        CommandLineExec.executeCommand( cmdArgs, text ) ;
        
        boolean paragraphAdded = false ;
        
        sb.append( "\n\n------- Page " + imgFile.getName() + " ----------\n\n" ) ;
        for( String line : text ) {
            if( !StringUtil.isEmptyOrNull( line ) ) {
                sb.append( line.trim() + " " ) ;
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
        args.add( "/usr/local/bin/tesseract" ) ;
        args.add( imgFile.getAbsolutePath() ) ;
        args.add( "stdout" ) ;
        args.add( "-l" ) ;
        args.add( lang ) ;
        return args.toArray( new String[args.size()] ) ;
    }
}
