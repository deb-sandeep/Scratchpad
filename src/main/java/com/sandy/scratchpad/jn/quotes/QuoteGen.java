package com.sandy.scratchpad.jn.quotes;

import java.io.File ;
import java.util.ArrayList ;
import java.util.List ;
import java.util.Map ;
import java.util.TreeMap ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class QuoteGen {

    private static final Logger log = Logger.getLogger( QuoteGen.class ) ;
    
    private static final String QUOTES_FILE_PATH = 
            "/Users/sandeep/Documents/StudyNotes/JoveNotes-Std-9/Class-9/English/99 - Quotes/doc/ocr.txt" ;
    
    private Map<String, List<Quote>> sections = new TreeMap<>() ;
    private Map<String, List<Quote>> speakers  = new TreeMap<>() ;

    public static void main( String[] args ) throws Exception {
        QuoteGen app = new QuoteGen() ;
        app.parseQuotes() ;
    }
    
    private void parseQuotes() throws Exception {
        
        List<String> lines = FileUtils.readLines( new File( QUOTES_FILE_PATH ) ) ;
        String currentSection = null ;
        
        for( String line : lines ) {
            if( StringUtil.isNotEmptyOrNull( line ) ) {
                line = line.trim() ;
                if( !line.startsWith( "* " ) ) {
                    currentSection = line ;
                }
                else {
                    line = line.substring( 2 ) ;
                    int index = line.lastIndexOf( " -" ) ;
                    if( index == -1 ) {
                        log.debug( "\n" + currentSection ) ;
                        log.debug( "   " + line  ) ;
                    }
                    else {
                        String quote = line.substring( 0, index ).trim() ;
                        String speaker = line.substring( index+2 ).trim() ;
                        
                        Quote q = new Quote( currentSection, quote, speaker ) ;
                        
                        addToMap( currentSection, sections, q ) ;
                        addToMap( speaker,        speakers, q ) ;
                    }
                }
            }
        }
        
        for( String key : sections.keySet() ) {
            //log.debug( key ) ; 
            printQuotesList( key, sections ) ;
        }
    }
    
    private void addToMap( String key, Map<String, List<Quote>> map, Quote q ) {
        
        List<Quote> list = map.get( key ) ;
        if( list == null ) {
            list = new ArrayList<>() ;
            map.put( key, list ) ;
        }
        
        list.add( q ) ;
    }
    
    private void printQuotesList( String key, Map<String, List<Quote>> map ) {
        List<Quote> list = map.get( key ) ;
        for( Quote q : list ) {
            //log.debug( "   " + q.getQuote() + "[" + q.getSpeaker() + "]" ) ;
            log.debug( q.getInsertQuery() ) ;
        }
    }
}
