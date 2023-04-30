package com.sandy.scratchpad.jn.poem;

import java.util.ArrayList ;
import java.util.List ;

public class PoemParagraphFIB {
    
    public static final int MIN_BLANK_LINE_HALF_WIDTH = 1 ;  
    public static final int MIN_BLANK_LINE_WIDTH_RND_WIDTH = 2 ;
    public static final float PCT_SINGLE_BLANK_LINE_GROUPS = 1 ; 
    
    private String section = null ;
    private List<LineGroup> groups = new ArrayList<>() ;
    private List<LineGroup> blankLineGroups = new ArrayList<>() ;
    
    public PoemParagraphFIB( String section, List<String> lines ) throws Exception {
        this.section = section ;
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
            blankLineGroups.add( group ) ;
        }
    }
    
    public void createFIBs() {
        
        if( this.section != null ) {
            System.out.println( "\n@section \"Paragraph - " + this.section + "\"\n" ) ;
        }
        
        for( LineGroup group : groups ) {
            group.generateFIB() ;
            System.out.println() ;
        }
        System.out.println( "// Num FIB = " + groups.size() ) ;
    }

    public void createBlankLineFIBs() {

        if( this.section != null ) {
            System.out.println( "\n@section \"Paragraph (L) - " + this.section + "\"\n" ) ;
        }

        for( LineGroup group : blankLineGroups ) {
            group.generateFIB() ;
            System.out.println() ;
        }
        System.out.println( "// Num FIB = " + groups.size() ) ;
    }

}
