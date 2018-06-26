package com.sandy.scratchpad.jn.poem;

import java.io.File ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.io.IOUtils;

public class PoemFIB {
    
    private File file = null ;
    private ArrayList<LineGroup> groups = new ArrayList<PoemFIB.LineGroup>() ;
    
    class LineGroup {
        int startLine = 0 ;
        int numLines  = 0 ;
        int blankLineIndex = -1 ;
        
        List<String> lines = new ArrayList<String>() ;
        
        private ArrayList<String> qLines  = new ArrayList<String>() ;
        private ArrayList<String> answers = new ArrayList<String>() ;
        private List<String> ineligibleWords = null;
        
        LineGroup( int start, int numLines ) throws Exception {
            this.startLine = start ;
            this.numLines  = numLines ;
            initIneligibleWordList();
        }
        
        LineGroup( int start, int numLines, int blankLineIndex ) throws Exception {
            this( start, numLines ) ;
            this.blankLineIndex = blankLineIndex ;
        }
        
        void initIneligibleWordList() throws Exception {
        	ineligibleWords = IOUtils.readLines(
		    		PoemFIB.class.getResourceAsStream( "/ineligibleWords.txt" )  
    		);
        }
        
        void extractBlanks() {
            
            if( this.blankLineIndex == -1 ) {
                for( String line : lines ) {
                    StringBuffer qLine = new StringBuffer() ;
                    String[] words = line.split( "\\s+" ) ;
                    
                    for( String word : words ) {
                        if( word.length() <= 3 ) {
                            qLine.append( word + " " ) ;
                        }
                        else {
                            if( Math.random() > 0.6 && isEligibleForBlank( word ) ) {
                                answers.add( word ) ;
                                qLine.append( "{" + (answers.size()-1) + "} " ) ;
                            }
                            else {
                                qLine.append( word + " ") ;
                            }
                        }
                    }
                    qLines.add( qLine.toString() ) ;
                }
            }
            else {
                for( int i=0; i<lines.size(); i++ ) {
                    String line = lines.get( i ) ;
                    
                    StringBuffer qLine = new StringBuffer() ;
                    String[] words = line.split( "\\s+" ) ;
                    
                    for( String word : words ) {
                        if( i != blankLineIndex ) {
                            qLine.append( word ).append( " " ) ;
                        }
                        else {
                            answers.add( word ) ;
                            qLine.append( "{" + (answers.size()-1) + "} " ) ;
                        }
                    }
                    qLines.add( qLine.toString() ) ;
                }
            }
        }
        
        boolean isEligibleForBlank( String word ) {
        	if( ineligibleWords.contains( word.toLowerCase() ) ) {
        		return false;
        	}
        	return true;
        }
        
        boolean isValid() {
            return !answers.isEmpty() ;
        }
        
        void generateFIB() {
            System.out.println( "@fib \"" ) ;
            for( String qLine : qLines ) {
                System.out.println( "### " + qLine.replaceAll( "\"", "\\\\\\\"" ) ) ;
            }
            System.out.println( "\"" ) ;
            for( String answer : answers ) {
                System.out.println( "\"" + answer.replaceAll( "\"", "\\\\\\\"" ) + "\"" ) ;
            }
        }
    }
    
    public PoemFIB( String filePath ) throws Exception {
        this.file = new File( filePath ) ;
    }
    
    private void initialize() throws Exception {
        if( !file.exists() ) {
            throw new Exception( "File " + file.getAbsolutePath() + 
                                 "  does not exist." ) ;
        }
        
        List<String> lines = FileUtils.readLines( file ) ;
        createLineGroups( lines ) ;
        createBlankLineGroups( lines ) ;
    }
    
    private void createLineGroups( List<String> lines ) throws Exception {
        
        int numCycles = 0 ;

        while( groups.size() < lines.size()*2 ) {
            
            numCycles++ ;
            
            int randomStartLine = 0 ;
            int randomGroupLen  = 2 + (int)(Math.random()*3) ;
            
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
            
            if( (numCycles - groups.size()) > 100 ) break ;
        }
    }
    
    private void createBlankLineGroups( List<String> lines ) throws Exception {
        for( int i=0; i<lines.size(); i++ ) {
            
            int randPrevLines = (int)( 1 + Math.random() * 2 ) ;
            int randNextLines = (int)( 1 + Math.random() * 2 ) ;
            
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
    }

    public static void main( String[] args ) throws Exception {
        System.out.println( "----------------------------------" ) ;
        PoemFIB driver = new PoemFIB( "/home/sandeep/temp/poem.txt" ) ;
        driver.initialize() ;
        driver.createFIBs() ;
    }

}
