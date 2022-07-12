package com.sandy.scratchpad.jn.englang.pronoun;

import java.io.BufferedReader ;
import java.io.InputStream ;
import java.io.InputStreamReader ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class PronounQAGenerator {
    
    private static final Logger log = Logger.getLogger( PronounQAGenerator.class ) ;
    
    public List<Line> loadLines() throws Exception {
        
        List<Line> lines = new ArrayList<>() ;
        
        InputStream is = PronounQAGenerator.class.getResourceAsStream( "/prose/pronoun-sentences.txt" ) ;
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) ;
        
        String line = null ;
        while( (line = reader.readLine()) != null ) {
            
            if( StringUtil.isNotEmptyOrNull( line ) ) {
                log.debug( line ) ;
                lines.add( new Line( line ) ) ;
            }
        }
        
        return lines ;
    }
    
    
    public static void main( String[] args ) throws Exception {
        
        new PronounQAGenerator().loadLines() ;
    }
}
