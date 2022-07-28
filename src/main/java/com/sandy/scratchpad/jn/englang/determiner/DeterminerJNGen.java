package com.sandy.scratchpad.jn.englang.determiner;

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
import com.sandy.scratchpad.jn.englang.determiner.Line.Segment ;

public class DeterminerJNGen {
    
    private static final Logger log = Logger.getLogger( DeterminerJNGen.class ) ;
    
    private Map<String, Map<String, List<Line>>> classifiedLines = new HashMap<>();
    
    public void loadLines() throws Exception {
        
        InputStream is = DeterminerJNGen.class.getResourceAsStream( "/prose/pronoun-sentences.txt" ) ;
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
            Determiner d = segment.determiner ;
            String typeName = d.getType().getName() ;
            
            Map<String, List<Line>> map = classifiedLines.get( typeName ) ;
            if( map == null ) {
                map = new HashMap<>() ;
                classifiedLines.put( typeName, map ) ;
            }
            
            List<Line> lines = map.get( d.getWord() ) ;
            if( lines == null ) {
                lines = new ArrayList<>() ;
                map.put( d.getWord(), lines ) ;
            }
            
            if( !lines.contains( line ) ) {
                lines.add( line ) ;
            }
        }
    }
    
    private void printHistogram() {
        
        DeterminerMaster pm = DeterminerMaster.instance() ;
        
        for( DeterminerType type : pm.getDeterminerTypes() ) {
        
            Map<String, List<Line>> map = classifiedLines.get( type.getName() ) ;
            if( map != null ) {
                
                int num = 0 ;
                StringBuilder sb = new StringBuilder() ;

                for( String pronoun : map.keySet() ) {
                    
                    List<Line> lines = map.get( pronoun ) ;
                    if( lines.size() > 0 ) {
                        Collections.shuffle( lines ) ;
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
        
        DeterminerMaster pm = DeterminerMaster.instance() ;
        for( DeterminerType type : pm.getDeterminerTypes() ) {
            
            Map<String, List<Line>> map = classifiedLines.get( type.getName() ) ;
            if( map != null ) {
                generateQuestions( type, map ) ;
            }
        }
    }
    
    private void generateQuestions( DeterminerType type, 
                                    Map<String, List<Line>> map ) {
        
        log.debug( "Generating question for " + type.getName() + " pronouns." ) ;
        int NUM_Q_PER_DETERMINER = 10 ;
        
        for( int i=0; i<NUM_Q_PER_DETERMINER; i++ ) {
            for( String determiner : map.keySet() ) {
                List<Line> lines = map.get( determiner ) ;
                if( i < lines.size() ) {
                    Line line = lines.get( i ) ;
                    generateQuestions( type, determiner, line ) ;
                }
            }
        }
    }
    
    private void generateQuestions( DeterminerType type, String pronoun, 
                                    Line line ) {
        
        log.debug( "\nGenerating identifying question for " + 
                   type.getName() + " determiner." ) ;
     
        QGenHelper questionGen = new QGenHelper( line ) ;
        questionGen.generateIdentifyQuestion( type ) ;
        questionGen.generateFIB() ;
    }
    
    public static void main( String[] args ) throws Exception {
        
        DeterminerJNGen gen = new DeterminerJNGen() ;
        gen.loadLines() ;
        gen.printHistogram() ;
        gen.generateQuestions() ;
    }
}
