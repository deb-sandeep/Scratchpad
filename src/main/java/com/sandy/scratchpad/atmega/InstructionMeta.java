package com.sandy.scratchpad.atmega;

import java.net.URL ;

import org.apache.commons.io.IOUtils ;
import org.apache.log4j.Logger ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.select.Elements ;

public class InstructionMeta {
    
    static final Logger log = Logger.getLogger( InstructionMeta.class ) ;
    
    public String   mnemonic        = null ;
    public String[] operands        = null ;
    public String   description     = "" ;
    public String   longDescription = "" ;
    public String   example         = "" ;
    public String   operation       = null ;
    public String[] flags           = null ;
    public int      cycles          = 1 ;

    public InstructionMeta( Elements cells ) throws Exception {
        
        mnemonic    = cells.get( 0 ).text().trim() ;
        operands    = cells.get( 1 ).text().split( "," ) ;
        description = cells.get( 2 ).text() ;
        operation   = cells.get( 3 ).text() ;
        flags       = cells.get( 4 ).text().split( "," ) ;
        cycles      = Integer.parseInt( cells.get( 5 ).text() ) ;
        
        populateDetails() ;
    }
    
    private void populateDetails() throws Exception {
        
        String pageName = mnemonic.replace( '\u00a0', ' ' ).trim() + ".html" ;
        
        URL url = new URL( "http://avr.8b.cz/asmhelp/Html/" + pageName ) ;
        String content = IOUtils.toString( url.openStream() ) ;
        content = sanitizeContent( content ) ;
        
        Document doc = Jsoup.parse( content ) ;
        Element body = doc.body() ;
        
        populateLongDescription( body ) ;
        populateExample( body ) ;
    }
    
    private String sanitizeContent( String input ) {
        
        String output = input.replace( "&quot;", "" ) ;
        output = output.replace( "CLASS\r\n", "CLASS=" ) ;
        return output ;
    }
    
    private void populateLongDescription( Element body ) 
        throws Exception {
        
        Elements sections = body.select( ".body-text" ) ;
        for( Element section : sections ) {
            longDescription += section.text() ;
            longDescription += "\n\n" ;
        }
        longDescription = longDescription.trim() ;
    }
    
    private void populateExample( Element body ) {
        
        Elements codeLines = body.select( "p.Code[style*=monospace]" ) ;
        for( Element codeLine : codeLines ) {
            example += codeLine.text() ;
            example += "\n" ;
        }
        example = example.trim() ;
    }
    
    public String toString() {
        
        StringBuilder buffer = new StringBuilder() ;
        
        buffer.append( "Mnemonic = " ).append( mnemonic ).append( "\n" ) ;
        buffer.append( "Operands = " ).append( String.join( ",", operands ) ).append( "\n" ) ;
        buffer.append( "Operation= " ).append( operation ).append( "\n" ) ;
        buffer.append( "Descr    = " ).append( description ).append( "\n" ) ;
        buffer.append( "\n" ) ;
        buffer.append( longDescription ) ;
        buffer.append( "\n" ) ;
        buffer.append( example ) ;
        buffer.append( "\n" ) ;
        
        return buffer.toString() ;
    }
}
