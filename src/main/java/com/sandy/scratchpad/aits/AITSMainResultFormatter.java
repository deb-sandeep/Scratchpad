package com.sandy.scratchpad.aits;

import java.io.File ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class AITSMainResultFormatter {
    
    private static final Logger log = Logger.getLogger( AITSMainResultFormatter.class ) ;
    
    public void execute() throws Exception {
        List<String> inputLines = getInputLines() ;
        log.debug( "--------------------" );
        for( int i=0; i<inputLines.size(); i+=2 ) {
            String paperId = inputLines.get( i ) ;
            String answers = inputLines.get( i+1 ) ;
            
            if( paperId.startsWith( "FJ17" ) ) {
                parseFJ17( paperId, answers.split( "\\$" ) ) ;
            }
            else {
                parseFJ18( paperId, answers.split( "\\$" ) ) ;
            }
        }
    }
    
    private void parseFJ17( String paperId, String[] answers ) {
        for( String ans : answers ) {
            if( StringUtil.isNotEmptyOrNull( ans ) ) {
                String qNo = ans.substring( 0, ans.indexOf( "." ) ) ;
                String[] qAnswers = ans.substring( ans.indexOf( "." ) + 1 ).split( "\\s+" ) ;
                
                printQImgName( "Phy" , qNo, paperId, qAnswers[1] ) ;
                printQImgName( "Chem", qNo, paperId, qAnswers[2] ) ;
                printQImgName( "Math", qNo, paperId, qAnswers[3] ) ;
            }
        }
    }
    
    private void parseFJ18( String paperId, String[] answers ) {
        
        for( String ans : answers ) {
            if( StringUtil.isNotEmptyOrNull( ans ) ) {
                String qNo = ans.substring( 0, ans.indexOf( "." ) ) ;
                String answer = ans.substring( ans.indexOf( "." ) + 1 ).trim() ;
                
                String sub = "Phy" ;
                int qNum = Integer.parseInt( qNo ) ;
                if( qNum >= 31 && qNum <= 60 ) {
                    sub = "Chem" ;
                    qNum-=30 ;
                }
                else if( qNum >= 61 && qNum <= 90 ) {
                    sub = "Math" ;
                    qNum-=60 ;
                }
                
                printQImgName( sub , Integer.toString( qNum ), paperId, answer ) ;
            }
        }
    }
    
    private void printQImgName( String sub, String qNo, String paperId, String ans ) {
        log.debug( sub + "_Q_" + paperId + "_" + qNo + "=" + ans ) ; 
    }
    
    private List<String> getInputLines() throws Exception {
        File file = new File( "/home/sandeep/temp/img.txt" ) ;
        List<String> lines = FileUtils.readLines( file ) ;
        return lines ;
    }

    public static void main( String[] args ) 
        throws Exception {
        new AITSMainResultFormatter().execute() ;
    }
}
