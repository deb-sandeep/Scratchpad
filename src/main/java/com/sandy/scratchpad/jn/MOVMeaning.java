package com.sandy.scratchpad.jn;

import java.io.File ;
import java.io.FileInputStream ;
import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;
import java.util.Properties ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.StringUtils ;
import org.apache.commons.lang.WordUtils ;

public class MOVMeaning {

    private File textFile = new File( "/home/sandeep/temp/text.txt" ) ;
    private File meaningFile = new File( "/home/sandeep/temp/meaning.txt" ) ;
    
    private Map<String, String> meanings = new HashMap<String, String>() ;
    private List<String> textLines = null ;
    
    public MOVMeaning() throws Exception {
        List<String> lines = FileUtils.readLines( meaningFile ) ;
        for( String line : lines ) {
            String[] parts = line.split( "=" ) ;
            meanings.put( parts[0].trim(), parts[1].trim() ) ;
        }
        textLines = FileUtils.readLines( textFile ) ;
    }
    
    private void findLineContainingWords( String words, List<String> lines ) {
        String lineSmallCase = null ;
        String wordSmallCase = words.toLowerCase() ;
        
        for( int i=0; i<textLines.size(); i++ ) {
            lineSmallCase = textLines.get( i ).toLowerCase() ;
            if( lineSmallCase.contains( wordSmallCase ) ) {
                if( i > 0 ) {
                    lines.add( textLines.get( i-1 ) ) ;
                }
                String lineWithWords = textLines.get( i ) ;
                lineWithWords = lineWithWords.replace( words, "`" + words + "`" ) ;
                lines.add( lineWithWords ) ;
                if( i < textLines.size()-1 ) {
                    lines.add( textLines.get( i+1 ) ) ;
                }
                break ;
            }
        }
    }
    
    private void findLines() {
        int index=0 ;
        for( String key : meanings.keySet() ) {
            List<String> lines = new ArrayList<String>() ;
            findLineContainingWords( key, lines ) ;
            
            //System.out.println( StringUtils.repeat( "-", 80 ) ) ;
            //System.out.println( "=> " + key ) ;
            if( !lines.isEmpty() ) {
                System.out.println( "@qa \"What is the meaning of `" + key + "` in the following sentences:" ) ;
                System.out.println( "" ) ;
                System.out.println( "-----------------------------------------------\n" ) ;
                for( int i=0; i<lines.size(); i++ ) {
                    System.out.print( "#### " + lines.get( i ) ) ;
                    if( i == lines.size()-1 ) {
                        System.out.println( "\"" ) ;
                    }
                    else {
                        System.out.println() ;
                    }
                }
                System.out.println( "\"" + WordUtils.wrap( meanings.get( key ), 80 ) + "\"" ) ;
                System.out.println() ;
            }
            else {
                System.out.println( StringUtils.repeat( "-", 80 ) ) ;
                System.out.println( "=> " + key ) ;
                System.out.println( "NO TEXT FOUND" ) ;
            }
        }
    }
    
    public static void main( String[] args ) throws Exception {
        MOVMeaning driver = new MOVMeaning() ;
        driver.findLines() ;
    }
}
