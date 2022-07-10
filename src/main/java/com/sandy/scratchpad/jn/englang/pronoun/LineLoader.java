package com.sandy.scratchpad.jn.englang.pronoun;

import java.io.BufferedReader ;
import java.io.InputStream ;
import java.io.InputStreamReader ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class LineLoader {
    
    private static final Logger log = Logger.getLogger( LineLoader.class ) ;
    
    public List<String> loadLines() throws Exception {
        
        List<String> lines = new ArrayList<>() ;
        
        InputStream is = LineLoader.class.getResourceAsStream( "/prose/pronoun-sentences.txt" ) ;
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) ;
        
        String line = null ;
        while( (line = reader.readLine()) != null ) {
            
            if( StringUtil.isNotEmptyOrNull( line ) ) {
                log.debug( line ) ;
                lines.add( line ) ;
            }
        }
        
        return lines ;
    }
    
    public static void main( String[] args ) throws Exception {
        
        PronounMaster.instance() ;
    }
}
