package com.sandy.scratchpad.jn.textextract;

import com.sandy.common.util.CommandLineExec;
import com.sandy.common.util.StringUtil;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class FolderTextExtractor {

    private static final Logger log = Logger.getLogger( FolderTextExtractor.class ) ;
    
    public static File IMG_DIR = new File(
            "/Users/sandeep/Documents/Scans/Essay and letters" ) ;
    
    public static void main( String[] args ) throws Exception {
        new FolderTextExtractor().processImgDir(); ;
    }
    
    private void processImgDir() throws Exception {
        
        File ocrFile = new File( IMG_DIR, "ocr-text.txt" ) ;

        List<File> imgFiles = getImgFiles( IMG_DIR ) ;
        StringBuilder sb = new StringBuilder() ;
        for( File file : imgFiles ) {
            log.debug( "    " + file.getName() ) ;
            collectImgText( file, sb ) ;
        }
        
        FileUtils.write( ocrFile, sb.toString(), "UTF-8", true ) ;
    }
    
    private List<File> getImgFiles( File imgDir ) {
        
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
    
    private int getFileSequence( File file ) {
        
        String fileName = file.getName() ;
        fileName = fileName.substring( 0, fileName.length()-4 ) ;
        
        String[] parts = fileName.split( "_" ) ;
        return Integer.parseInt( parts[1] );
    }
    
    private void collectImgText( File imgFile, StringBuilder sb ) {
        
        String[] cmdArgs = generateCmdArgs( imgFile, "eng" ) ;
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
