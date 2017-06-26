package com.sandy.scratchpad.atmega;

import java.io.File ;
import java.net.URL ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.io.IOUtils ;
import org.apache.log4j.Logger ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.select.Elements ;

public class InstructionSet {
    
    static final Logger log = Logger.getLogger( InstructionSet.class ) ;
    
    private static final String LANDING_PG_URL = 
           "http://avr.8b.cz/asmhelp/Html/ATmega32_ATmega16_instructions.html" ;
    
    public InstructionSet() {
    }
    
    public void execute() throws Exception {
        processLandingPage() ;
    }
    
    private void processLandingPage() throws Exception {
        URL url = new URL( LANDING_PG_URL ) ;
        Document doc = Jsoup.parse( url, 5000 ) ;
        Element body = doc.body() ;
        
        Elements sections = body.select( "h3" ) ;
        for( Element section : sections ) {
            String sectionName = section.text() ;
            Element table = section.nextElementSibling() ;
            
            processSection( sectionName, table ) ;
        }
    }
    
    private void processSection( String sectionName, Element table ) 
        throws Exception {
        
        List<InstructionMeta> instructions = new ArrayList<InstructionMeta>() ;
        
        Elements rows = table.select( "tr" ) ;
        for( int rowId=0; rowId<rows.size(); rowId++ ) {
            if( rowId == 0 )continue ;
            
            Element row = rows.get( rowId ) ;
            InstructionMeta meta = new InstructionMeta( row.select( "td" ) ) ;
            instructions.add( meta ) ;
        }
        
        makeNotes( sectionName, instructions ) ;
    }
    
    private void makeNotes( String sectionName, 
                            List<InstructionMeta> instructions )
        throws Exception {
        
        File file = new File( "/home/sandeep/temp/" + sectionName + ".txt" ) ;
        StringBuilder buffer = new StringBuilder() ;
        
        makeTeacherNotes( instructions, buffer ) ;
        makeQuestions( instructions, buffer ) ;
        
        FileUtils.write( file, buffer.toString() ) ;
    }
    
    private void makeTeacherNotes( List<InstructionMeta> instructions,
                                   StringBuilder buffer ) {
        
    }
    
    private void makeQuestions( List<InstructionMeta> instructions,
                                StringBuilder buffer ) {
        
    }

    public static void main( String[] args ) throws Exception {
        InstructionSet engine = new InstructionSet() ;
        engine.execute() ;
    }
}
