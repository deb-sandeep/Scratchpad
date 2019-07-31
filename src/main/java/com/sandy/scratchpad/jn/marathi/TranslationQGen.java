package com.sandy.scratchpad.jn.marathi;

import java.io.File ;
import java.util.ArrayList ;
import java.util.Iterator ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.StringUtils ;
import org.apache.log4j.Logger ;

public class TranslationQGen {
    
    static Logger log = Logger.getLogger( TranslationQGen.class ) ;
    
    private File file = null ;
    private ArrayList<LineGroup> groups = new ArrayList<TranslationQGen.LineGroup>() ;
    
    class LineGroup {
        int lineNumber = 0 ;
        List<String> lines = new ArrayList<String>() ;
        
        LineGroup( int lineNum ) {
            this.lineNumber = lineNum/2 + 1 ;
        }
        
        void addLine( String line ) {
            lines.add( line ) ;
        }
    }
    
    public TranslationQGen( String filePath ) throws Exception {
        this.file = new File( filePath ) ;
    }
    
    private void initialize() throws Exception {
        if( !file.exists() ) {
            throw new Exception( "File " + file.getAbsolutePath() + 
                                 "  does not exist." ) ;
        }
        
        List<String> lines = FileUtils.readLines( file ) ;
        
        int lineNumber = 0 ;
        LineGroup curGroup = null ;
        for( Iterator<String> iter = lines.iterator(); iter.hasNext(); ) {
            String line = iter.next() ;
            if( StringUtils.isEmpty( line.trim() ) ) {
                iter.remove() ;
                continue ;
            }
            lineNumber++ ;
            
            if( lineNumber % 2 == 1 ) {
                curGroup = new LineGroup( lineNumber ) ;
                curGroup.addLine( line ) ;
            }
            else {
                curGroup.addLine( line ) ;
                groups.add( curGroup ) ;
            }
        }
    }
    
    private void createNotes() {
        StringBuffer buffer = new StringBuffer() ;
        for( int i=0; i<groups.size(); i++ ) {
            LineGroup group = groups.get( i ) ;
            String audioClip = String.format( "audio-L2-%02d.mp3", group.lineNumber ) ;
            
            buffer.append( "@qa \"## " + group.lines.get( 1 ) )
                  .append( "\n\n" )
                  .append( "Translate to marathi\"\n" )
                  .append( "\"{{audio " + audioClip + "}}\n\n" )
                  .append( "## " + group.lines.get( 0 ) + "\"\n" )
                  .append( "\n" ) ;
        }
        
        log.debug( buffer.toString() ) ;
    }
    
    public static void main( String[] args ) throws Exception {
        TranslationQGen driver = new TranslationQGen( "/home/sandeep/temp/text-translation.txt" ) ;
        driver.initialize() ;
        driver.createNotes() ;
    }
}
