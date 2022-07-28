package com.sandy.scratchpad.jn.englang.preposition;

import java.io.BufferedReader ;
import java.io.InputStream ;
import java.io.InputStreamReader ;
import java.util.ArrayList ;
import java.util.Collections ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;
import com.sandy.scratchpad.jn.englang.preposition.Line.PrepositionSegment ;

public class PrepositionJNGen {
    
    private static final Logger log = Logger.getLogger( PrepositionJNGen.class ) ;
    
    private Map<String, List<Line>> classifiedLines = new HashMap<>();
    
    public void loadLines() throws Exception {
        
        InputStream is = PrepositionJNGen.class.getResourceAsStream( "/prose/determiner-sentences.txt" ) ;
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) ;
        
        String line = null ;
        
        while( (line = reader.readLine()) != null ) {
            
            if( StringUtil.isNotEmptyOrNull( line ) ) {
                
                Line lineObj = new Line( line ) ;
                if( lineObj.getNumPrepositions() > 0 ) {
                    classifyLine( lineObj ) ;
                }
            }
        }
    }
    
    private void classifyLine( Line line ) {
        
        List<PrepositionSegment> segments = line.getPrepositionSegments() ;
        
        for( PrepositionSegment segment : segments ) {
            
            List<Line> lines = classifiedLines.get( segment.preposition ) ;
            if( lines == null ) {
                lines = new ArrayList<>() ;
                classifiedLines.put( segment.preposition, lines ) ;
            }
            
            if( !lines.contains( line ) ) {
                lines.add( line ) ;
            }
        }
    }
    
    private void printHistogram() {
        
        for( String preposition : classifiedLines.keySet() ) {
            List<Line> lines = classifiedLines.get( preposition ) ;
            Collections.shuffle( lines ) ;
            if( lines.size() > 0 ) {
                log.debug( "  " + preposition + " = " + lines.size() ) ;
            }
        }
    }
    
    private void generateQuestions() {
        
        QGenHelper helper = null ;
        int numQ = 0 ;
        
        for( int i=0; i<10; i++ ) {
            for( String preposition : classifiedLines.keySet() ) {
                List<Line> lines = classifiedLines.get( preposition ) ;
                if( i < lines.size() ) {
                    helper = new QGenHelper( lines.get( i ) ) ;
                    helper.generateFIB() ;
                    numQ++ ;
                }
            }
        }
        
        log.info( "Num questions = " + numQ ) ;
    }
    
    public static void main( String[] args ) throws Exception {
        
        PrepositionJNGen gen = new PrepositionJNGen() ;
        gen.loadLines() ;
        gen.printHistogram() ;
        gen.generateQuestions() ;
    }
}
