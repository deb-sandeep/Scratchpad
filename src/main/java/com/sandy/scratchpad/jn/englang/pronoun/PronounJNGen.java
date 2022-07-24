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

public class PronounJNGen {
    
    private static final Logger log = Logger.getLogger( PronounJNGen.class ) ;
    
    private Map<String, Map<String, List<Line>>> classifiedLines = new HashMap<>();
    
    public void loadLines() throws Exception {
        
        InputStream is = PronounJNGen.class.getResourceAsStream( "/prose/pronoun-sentences.txt" ) ;
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) ;
        
        String line = null ;
        
        while( (line = reader.readLine()) != null ) {
            
            if( StringUtil.isNotEmptyOrNull( line ) ) {
                
                Line lineObj = new Line( line ) ;
                if( lineObj.getNumPronouns() > 0 ) {
                    classifyLine( lineObj ) ;
                }
            }
        }
    }
    
    private void classifyLine( Line line ) {
        
        List<Segment> segments = line.getPronouns() ;
        
        for( Segment segment : segments ) {
            Pronoun p = segment.pronoun ;
            String pronounType = p.getType().getName() ;
            
            Map<String, List<Line>> map = classifiedLines.get( pronounType ) ;
            if( map == null ) {
                map = new HashMap<>() ;
                classifiedLines.put( pronounType, map ) ;
            }
            
            List<Line> lines = map.get( p.getWord() ) ;
            if( lines == null ) {
                lines = new ArrayList<>() ;
                map.put( p.getWord(), lines ) ;
            }
            
            if( !lines.contains( line ) ) {
                lines.add( line ) ;
            }
        }
    }
    
    private void printHistogram() {
        
        PronounMaster pm = PronounMaster.instance() ;
        
        for( PronounType type : pm.getPronounTypes() ) {
        
            Map<String, List<Line>> map = classifiedLines.get( type.getName() ) ;
            if( map != null ) {
                
                int num = 0 ;
                StringBuilder sb = new StringBuilder() ;

                for( String pronoun : map.keySet() ) {
                    
                    List<Line> lines = map.get( pronoun ) ;
                    if( lines.size() > 0 ) {
                        num += lines.size() ;
                        sb.append( "  " + pronoun + " = " + lines.size() + "\n" ) ;
                    }
                }
                log.debug( type.getName() + " = " + num ) ;
                log.debug( sb ) ;
            }
        }
    }
    
    private void generateQuestions() {
        
        PronounMaster pm = PronounMaster.instance() ;
        for( PronounType type : pm.getPronounTypes() ) {
            
            Map<String, List<Line>> map = classifiedLines.get( type.getName() ) ;
            if( map != null ) {
                generateQuestions( type, map ) ;
            }
        }
    }
    
    private void generateQuestions( PronounType type, 
                                    Map<String, List<Line>> map ) {
        
        log.debug( "Generating question for " + type.getName() + " pronouns." ) ;
        int NUM_Q_PER_PRONOUN = 5 ;
        
        for( int i=0; i<NUM_Q_PER_PRONOUN; i++ ) {
            for( String pronoun : map.keySet() ) {
                List<Line> lines = map.get( pronoun ) ;
                if( i < lines.size() ) {
                    Line line = lines.get( i ) ;
                    generateQuestions( type, pronoun, line ) ;
                }
            }
        }
    }
    
    private void generateQuestions( PronounType type, String pronoun, Line line ) {
        
        log.debug( "\nGenerating identifying question for " + 
                type.getName() + " pronoun" ) ;
     
        PronounQGen questionGen = new PronounQGen( line ) ;
        questionGen.generateIdentifyQuestion( type ) ;
    }
    
    public static void main( String[] args ) throws Exception {
        
        PronounJNGen gen = new PronounJNGen() ;
        gen.loadLines() ;
        gen.printHistogram() ;
        gen.generateQuestions() ;
    }
}
