package com.sandy.scratchpad.jn;

import java.io.File ;
import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.StringUtils ;
import org.apache.commons.lang.WordUtils ;
import org.apache.log4j.Logger ;

public class MOVMeaning {

    private static final Logger log = Logger.getLogger( MOVMeaning.class ) ;
    
    private static File JN_MOV_DIR = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes/Class-9/Literature - MoV" ) ;
    
    private File folder = null ;
    private File textFile = null ;
    private File meaningFile = null ;
    private File jnFile = null ;
    
    private Map<String, String> meanings = new HashMap<String, String>() ;
    private List<String> textLines = new ArrayList<>() ;
    
    public MOVMeaning( int chpNum, int subChpNum, int actNum, int sceneNum ) 
            throws Exception {

        log.info( "Creating MoV meaning for :" ) ;
        log.info( "\tChapter number     = " + chpNum ) ;
        log.info( "\tSub chapter number = " + subChpNum );
        log.info( "\tAct number         = " + actNum );
        log.info( "\tScene number       = " + sceneNum );
        
        String folderName = createFolderName( chpNum, actNum, sceneNum ) ;
        
        this.folder = new File( JN_MOV_DIR, folderName ) ;
        this.textFile = new File( this.folder + "/doc/ocr.txt" ) ;
        this.meaningFile = new File( this.folder + "/doc/ocr-meanings.txt" ) ;
        checkFileExists( this.folder ) ;
        checkFileExists( this.textFile ) ;
        checkFileExists( this.meaningFile ) ;
        
        this.jnFile = getJNFile( chpNum, subChpNum, actNum, sceneNum ) ;

        log.info( "\nFile :" ) ;
        log.info( "\ttextFile    = " + this.textFile.getAbsolutePath() ) ;
        log.info( "\tmeaningFile = " + this.meaningFile.getAbsolutePath() );
        log.info( "\tjnFile      = " + this.jnFile.getAbsolutePath() ) ;
        log.info( "----------------------------------------------------\n" ) ;
        
        loadMeanings() ;
        loadTextLines() ;
    }
    
    private void checkFileExists( File file ) throws Exception {
        if( !file.exists() ) {
            throw new Exception( "File " + file.getAbsolutePath() + " does not exist." ) ;
        }
    }
    
    private String createFolderName( int chpNum, int actNum, int sceneNum ) {
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( StringUtils.leftPad( Integer.toString( chpNum ), 2, '0' ) )
              .append( " - " )
              .append( "Act" + actNum )
              .append( " - " )
              .append( "Scene " + sceneNum ) ;
        return buffer.toString() ;
    }
    
    private File getJNFile( int chpNum, int subChpNum, int actNum, int sceneNum ) 
        throws Exception {
        
        File jnFile = null ;
        StringBuilder buffer = new StringBuilder() ;
        
        buffer.append( chpNum )
              .append( "." )
              .append( subChpNum )
              .append( " - " )
              .append( "Act" + actNum )
              .append( " - " )
              .append( "Scene " )
              .append( sceneNum )
              .append( " (meaning).jn" ) ;
        
        jnFile = new File( this.folder, buffer.toString() ) ;
        
        if( !jnFile.exists() ) {
            initializeJNFile( jnFile, chpNum, subChpNum, actNum, sceneNum ) ;
        }
        
        return jnFile ;
    }
    
    private void initializeJNFile( File jnFile, int chpNum, int subChpNum, int actNum, int sceneNum ) 
        throws Exception {
        
        StringBuilder buffer = new StringBuilder() ;
        
        buffer.append( "@skip_generation_in_production" ).append( "\n" ) ;
        buffer.append( "" ).append( "\n" ) ;
        buffer.append( "subject \"Literature - MoV\"" ).append( "\n" ) ;
        buffer.append( "chapterNumber " + chpNum + "." + subChpNum ).append( "\n" ) ;
        buffer.append( "chapterName " ) ;
        buffer.append( "\"Act" + actNum + " - Scene " + sceneNum + " (meaning)\"" ).append( "\n" ) ;
        buffer.append( "\n" ) ;

        FileUtils.writeStringToFile( jnFile, buffer.toString() );
    }
    
    private void loadMeanings() throws Exception {
        
        List<String> lines = FileUtils.readLines( meaningFile ) ;
        for( String line : lines ) {
            if( shouldSkipLine( line ) ) continue ;
            
            log.debug( "Processing line for meaning." ) ;
            log.debug( "\t" + line ) ;
            
            String[] parts = line.split( ":" ) ;
            String key = parts[0].trim().replaceAll( "^[0-9]+\\.\\s+", "" ) ;
            String meaning = parts[1].trim() ;
            
            log.debug( "Adding meaning key=" + key.trim() + 
                                ", meaning=" + meaning ) ;
            meanings.put( key.trim(), meaning ) ;
        }
    }
    
    private void loadTextLines() throws Exception {
        
        List<String> lines = FileUtils.readLines( textFile ) ;
        for( String line : lines ) {
            line = line.trim() ;
            
            if( shouldSkipLine( line ) ) continue ;
            if( line.matches( "^[A-Z]+$" ) ) continue ;
            
            log.debug( "Adding line=" + line ) ;
            textLines.add( line ) ;
        }
    }
    
    private boolean shouldSkipLine( String line ) {
        
        line = line.trim() ;
        if( StringUtils.isEmpty( line ) ) {
            return true ;
        }
        if( line.startsWith( "#" ) ) {
            return true ;
        }
        if( line.startsWith( "-----" ) ) {
            return true ;
        }
        return false ;
    }
    
    private void findLines() {
        
        StringBuilder jnContent = new StringBuilder() ;
        List<String>  unMatchedWords = new ArrayList<>() ;
        
        for( String key : meanings.keySet() ) {
            List<String> lines = new ArrayList<String>() ;
            findLineContainingWords( key, lines ) ;
            
            if( !lines.isEmpty() ) {
                jnContent.append( "@qa \"What is the meaning of `" + key + "` in the following sentences:" )
                         .append( "\n" )
                         .append( "\n" )
                         .append( "-----------------------------------------------\n\n" ) ;
                
                for( int i=0; i<lines.size(); i++ ) {
                    jnContent.append( "#### " + lines.get( i ) ) ;
                    if( i == lines.size()-1 ) {
                        jnContent.append( "\"" ) ;
                    }
                    else {
                        jnContent.append( "\n" ) ;
                    }
                }
                jnContent.append( "\n" )
                         .append( "\"" + WordUtils.wrap( meanings.get( key ), 80 ) + "\"" )
                         .append( "\n\n" ) ;
            }
            else {
                unMatchedWords.add( key ) ;
            }
        }
        
        if( !unMatchedWords.isEmpty() ) {
            log.error( "Some meaning keys were not found in the text:" ) ;
            for( String key : unMatchedWords ) {
                log.error( "\n\t" + key ) ;
            }
        }
        else {
            log.info( jnContent ) ;
            try {
                FileUtils.writeStringToFile( jnFile, jnContent.toString(), true ) ;
            }
            catch( Exception e ) {
                log.error( "Could not write to jn file", e ) ;
            }
        }
    }
    
    private void findLineContainingWords( String words, List<String> lines ) {
        String lineSmallCase = null ;
        String wordSmallCase = words.toLowerCase() ;
        
        for( int i=0; i<textLines.size(); i++ ) {
            lineSmallCase = textLines.get( i ).toLowerCase() ;
            
            if( lineSmallCase.contains( wordSmallCase ) ) {
                if( i > 0 ) {
                    lines.add( textLines.get( i-1 ) ) ;
                }
                
                String lineWithWords = textLines.get( i ) ;
                
                int startPos = lineSmallCase.indexOf( wordSmallCase ) ;
                int endPos   = startPos + wordSmallCase.length() ;
                
                StringBuilder middleLine = new StringBuilder( lineWithWords ) ;
                middleLine.insert( startPos, "`" ) ;
                middleLine.insert( endPos+1, "`" ) ;
                
                lines.add( middleLine.toString() ) ;
                
                if( i < textLines.size()-1 ) {
                    lines.add( textLines.get( i+1 ) ) ;
                }
                break ;
            }
        }
    }
    
    public static void main( String[] args ) throws Exception {
        
        MOVMeaning driver = new MOVMeaning( 11, 3, 2, 4 ) ;
        driver.findLines() ;
    }
}
