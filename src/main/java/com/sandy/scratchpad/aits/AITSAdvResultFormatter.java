package com.sandy.scratchpad.aits;

import java.io.File ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.StringUtils ;
import org.apache.log4j.Logger ;



public class AITSAdvResultFormatter {
    
    private static final Logger log = Logger.getLogger( AITSAdvResultFormatter.class ) ;
    
    private Map<String, Map<String, Map<String, String>>> ansLookup = new HashMap<>() ;
    
    public void execute() throws Exception {
        List<String> inputLines = getInputLines() ;
        log.debug( "--------------------" );
        for( int i=0; i<inputLines.size(); i++ ) {
            String line = inputLines.get( i ).trim() ;
            if( line.equals( "" ) ) continue ;
            
            String paper = null ;
            if( line.startsWith( "FJ" ) ) {
                log.debug( "\nPaper - " + line );
                paper = line ;
            }
            else {
                String[] parts = line.split( "\\s+" ) ;
                printTupule( parts ) ;
                classifyAnswers( paper, parts ) ;
            }
        }
    }
    
    private void printTupule( String[] parts ) {
        StringBuilder builder = new StringBuilder() ;
        builder.append( StringUtils.rightPad( parts[0], 5 ) ).append( " :: " ) ;
        builder.append( StringUtils.rightPad( parts[1], 10 ) ).append( " :: " ) ;
        builder.append( StringUtils.rightPad( parts[2], 10 ) ).append( " :: " ) ;
        builder.append( StringUtils.rightPad( parts[3], 10 ) ).append( " :: " ) ;
        builder.append( parts[4] ) ;
        log.debug( builder.toString() ) ;
    }
    
    private void classifyAnswers( String paper, String[] parts ) {
        
        String qType   = parts[0].trim() ;
        String qNo     = parts[1].replace( ".", "" ).trim() ;
        String phyAns  = parts[2].trim() ;
        String chemAns = parts[3].trim() ;
        String mathAns = parts[4].trim() ;
        
        
    }
    
    private void classifyAnswer( String paper, String sub, String qType, String qNo, String ans ) {
    }
    
    private List<String> getInputLines() throws Exception {
        File file = new File( "/home/sandeep/projects/source/SConsoleProcessedImages/AITSAdvSols.txt" ) ;
        List<String> lines = FileUtils.readLines( file ) ;
        return lines ;
    }

    public static void main( String[] args ) 
        throws Exception {
        new AITSAdvResultFormatter().execute() ;
    }
}
