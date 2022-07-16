package com.sandy.scratchpad.jn.englang.pronoun;

import java.io.BufferedReader ;
import java.io.InputStream ;
import java.io.InputStreamReader ;
import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;
import com.sandy.scratchpad.jn.englang.pronoun.Line.Segment ;

public class PronounQAGenerator {
    
    private static final Logger log = Logger.getLogger( PronounQAGenerator.class ) ;
    
    private Map<String, List<Line>> classifiedLines = new HashMap<>();
    
    public void loadLines() throws Exception {
        
        InputStream is = PronounQAGenerator.class.getResourceAsStream( "/prose/pronoun-sentences.txt" ) ;
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) ;
        
        String line = null ;
        
        while( (line = reader.readLine()) != null ) {
            
            if( StringUtil.isNotEmptyOrNull( line ) ) {
                
                Line lineObj = new Line( line ) ;
                
                if( lineObj.getNumPronouns() > 0 ) {
                    
                    List<Segment> segments = lineObj.getPronouns() ;
                    
                    for( Segment segment : segments ) {
                        Pronoun p = segment.pronoun ;
                        
                        List<Line> lines = classifiedLines.get( p.getType().getName() ) ;
                        if( lines == null ) {
                            lines = new ArrayList<>() ;
                            classifiedLines.put( p.getType().getName(), lines ) ;
                        }
                        
                        if( !lines.contains( lineObj ) ) {
                            lines.add( lineObj ) ;
                        }
                    }
                }
            }
        }
    }
    
    private void printHistogram() {
        
        PronounMaster pm = PronounMaster.instance() ;
        for( PronounType type : pm.getPronounTypes() ) {
            List<Line> lines = classifiedLines.get( type.getName() ) ;
            int num = 0 ;
            if( lines != null ) {
                num = lines.size() ;
            }
            
            log.debug( type.getName() + " = " + num ) ;
        }
    }
    
    private void printLines( String pronounType ) {
        List<Line> lines = classifiedLines.get( pronounType ) ;
        for( Line line : lines ) {
            line.print() ;
        }
    }
    
    public static void main( String[] args ) throws Exception {
        
        PronounQAGenerator gen = new PronounQAGenerator() ;
        gen.loadLines() ;
        gen.printHistogram() ;
        gen.printLines( "Demonstrative" ) ;
    }
}
