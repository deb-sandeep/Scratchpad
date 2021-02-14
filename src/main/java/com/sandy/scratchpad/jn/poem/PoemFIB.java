package com.sandy.scratchpad.jn.poem;

import java.io.File ;
import java.util.ArrayList ;
import java.util.Iterator ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.StringUtils ;

public class PoemFIB {
    
    public static final int MIN_BLANK_LINE_HALF_WIDTH = 2 ;  
    public static final int MIN_BLANK_LINE_WIDTH_RND_WIDTH = 3 ;
    public static final float PCT_SINGLE_BLANK_LINE_GROUPS = 0.5f ; 
    
    private File file = null ;
    private ArrayList<LineGroup> groups = new ArrayList<LineGroup>() ;
    
    public PoemFIB( File file ) throws Exception {
        this.file = file ;
    }
    
    private void initialize() throws Exception {
        if( !file.exists() ) {
            throw new Exception( "File " + file.getAbsolutePath() + 
                                 "  does not exist." ) ;
        }
        
        List<String> lines = FileUtils.readLines( file ) ;
        
        for( Iterator<String> iter = lines.iterator(); iter.hasNext(); ) {
            String line = iter.next() ;
            if( StringUtils.isEmpty( line.trim() ) ) {
                iter.remove() ;
            }
        }
        
        createLineGroups( lines ) ;
        createBlankLineGroups( lines ) ;
    }
    
    private void createLineGroups( List<String> lines ) throws Exception {
        
        int maxGroups = (int)( lines.size()*PCT_SINGLE_BLANK_LINE_GROUPS ) ;
        int numCycles = 0 ;

        while( groups.size() < maxGroups ) {
            
            numCycles++ ;
            
            int randomStartLine = 0 ;
            int randomGroupLen  = (MIN_BLANK_LINE_HALF_WIDTH*2) + 
                                  (int)( Math.random()*MIN_BLANK_LINE_WIDTH_RND_WIDTH ) ;
            
            if( numCycles >= lines.size() ) {
                randomStartLine = (int)(Math.random()*lines.size()) ;
            }
            else {
                randomStartLine = numCycles-1 ;
            }
            
            if( randomStartLine + randomGroupLen >= lines.size() ) {
                randomGroupLen = lines.size() - randomStartLine ;
            }
            
            boolean skipGroup = false ;
            for( LineGroup group : groups ) {
                if( group.startLine == randomStartLine && 
                    group.numLines  == randomGroupLen ) {
                    skipGroup = true ;
                    break ;
                }
            }
            
            if( !skipGroup ) {
                LineGroup group = new LineGroup( randomStartLine, randomGroupLen ) ;
                for( int lineNum = randomStartLine; 
                         lineNum < randomStartLine + randomGroupLen; 
                         lineNum++ ) {
                    group.lines.add( lines.get( lineNum ) ) ;
                }
                group.extractBlanks() ;
                if( group.isValid() ) {
                    groups.add( group ) ;
                }
            }
            
            if( (numCycles - groups.size()) > 500 ) break ;
        }
    }
    
    private void createBlankLineGroups( List<String> lines ) throws Exception {
        
        for( int i=0; i<lines.size(); i+=(MIN_BLANK_LINE_HALF_WIDTH*2) ) {
            
            int randPrevLines = (int)( MIN_BLANK_LINE_HALF_WIDTH + 
                                       Math.random() * MIN_BLANK_LINE_WIDTH_RND_WIDTH ) ;
            int randNextLines = (int)( MIN_BLANK_LINE_HALF_WIDTH + 
                                       Math.random() * MIN_BLANK_LINE_WIDTH_RND_WIDTH ) ;
            
            int fromLine = i - randPrevLines ;
            int toLine   = i + randNextLines ;
            
            fromLine = ( fromLine < 0 ) ? 0 : fromLine ;
            toLine   = ( toLine >= lines.size() ) ? lines.size()-1 : toLine ;
            
            LineGroup group = new LineGroup( fromLine, toLine-fromLine, (i-fromLine) ) ;
            for( int lineIndex=fromLine; lineIndex<=toLine; lineIndex++ ) {
                group.lines.add( lines.get( lineIndex ) ) ;
            }
            group.extractBlanks() ;
            groups.add( group ) ;
        }
    }
    
    public void createFIBs() {
        for( LineGroup group : groups ) {
            group.generateFIB() ;
            System.out.println() ;
        }
        System.out.println( "// Num FIB = " + groups.size() ) ;
    }

    public static void main( String[] args ) throws Exception {
        File poemFile = getPoemFile() ;
        PoemFIB driver = new PoemFIB( poemFile ) ;
        driver.initialize() ;
        driver.createFIBs() ;
    }
    
    public static File getPoemFile() {
        
        File userHomeDir = new File( System.getProperty("user.home") ) ;
        return new File( userHomeDir, "poem.txt" ) ;
    }
}