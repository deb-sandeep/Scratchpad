package com.sandy.scratchpad.jn.poem;

import java.io.File ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;

public class PoemFIB {
    
    private File file = null ;
    private ArrayList<LineGroup> groups = new ArrayList<PoemFIB.LineGroup>() ;
    
    class LineGroup {
        int startLine = 0 ;
        int numLines  = 0 ;
        List<String> lines = new ArrayList<String>() ;
        
        private ArrayList<String> qLines  = new ArrayList<String>() ;
        private ArrayList<String> answers = new ArrayList<String>() ;
        
        LineGroup( int start, int numLines ) {
            this.startLine = start ;
            this.numLines  = numLines ;
        }
        
        void extractBlanks() {
            
            for( String line : lines ) {
                StringBuffer qLine = new StringBuffer() ;
                String[] words = line.split( "\\s+" ) ;
                
                for( String word : words ) {
                    if( word.length() <= 3 ) {
                        qLine.append( word + " " ) ;
                    }
                    else {
                        if( Math.random() > 0.7 ) {
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
        
        boolean isValid() {
            return !answers.isEmpty() ;
        }
        
        void generateFIB() {
            System.out.println( "@fib \"" ) ;
            for( String qLine : qLines ) {
                System.out.println( "### " + qLine ) ;
            }
            System.out.println( "\"" ) ;
            for( String answer : answers ) {
                System.out.println( "\"" + answer + "\"" ) ;
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
        createFileGroups( lines ) ;
    }
    
    private void createFileGroups( List<String> lines ) {
        
        while( groups.size() < 50 ) {
            
            int randomStartLine = (int)(Math.random()*lines.size()) ;
            int randomGroupLen  = 2 + (int)(Math.random()*3) ;
            
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
        }
    }
    
    public void createFIBs() {
        for( LineGroup group : groups ) {
            group.generateFIB() ;
            System.out.println() ;
        }
    }

    public static void main( String[] args ) throws Exception {
        PoemFIB driver = new PoemFIB( "/home/sandeep/temp/poem.txt" ) ;
        driver.initialize() ;
        driver.createFIBs() ;
    }

}
