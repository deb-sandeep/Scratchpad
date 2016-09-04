package com.sandy.scratchpad.jn.mov;

import java.io.File ;
import java.io.FileFilter ;
import java.net.URL ;
import java.util.Arrays ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.WordUtils ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.select.Elements ;

public class MoVInterpret {

    public void driveFromFiles() throws Exception {
        File[] sourceFiles = getHTMLFiles() ;
        Arrays.sort( sourceFiles ) ;
        for( File src : sourceFiles ) {
            
            String content = FileUtils.readFileToString( src ) ;
            Document doc = Jsoup.parse( content ) ;
            
            processBody( doc.body() ) ;
        }
    }
    
    public void driveFromURL( int startPageNo, int endPageNo ) throws Exception {
        
        for( int pageNo=startPageNo; pageNo<=endPageNo; pageNo += 2 ) {

            URL url = new URL( "http://nfs.sparknotes.com/merchant/page_" + pageNo + ".html" ) ;
            System.out.println( "// Parsing - " + url.toExternalForm() ) ;
            Document doc = Jsoup.parse( url, 5000 ) ;
            processBody( doc.body() ) ;
        }
    }
    
    private void processBody( Element body ) {
        
        Elements interpretRows = body.select( "table.noFear tr" ) ;
        for( Element row : interpretRows ) {
            
            Element original = row.select( "td.noFear-left" ).get( 0 ) ;
            Element interpretation = row.select( "td.noFear-right" ).get( 0 ) ;
            
            processInterpretation( original, interpretation ) ;
        }
    }
    
    private void processInterpretation( Element original, Element interpretation ) {

        String question = buildQuestion( original ) ;
        String answer   = buildAnswer( interpretation ) ;
        System.out.println( question ) ;
        System.out.println( answer ) ;
        System.out.println() ;
    }
    
    private String buildQuestion( Element original ) {
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( "@qa \"Interpret the following lines\n\n" ) ;
        
        Elements children = original.children() ;
        for( Element child : children ) {
            if( child.tagName().equalsIgnoreCase( "b" ) ) {
                buffer.append( ">  \n" )
                      .append( "> " + breakQuote( child.text().trim() ) ).append( "  \n" )
                      .append( ">  \n" );
            }
            else {
                buffer.append( "> " ).append( breakQuote( child.text().trim() ) ).append( "  \n" ) ;
            }
        }
        buffer.append( "\"" ) ;
        return buffer.toString() ;
    }
    
    private String breakQuote( String input ) {
        return WordUtils.wrap( input, 50, "  \n> ", false ) ;
    }
    
    private String buildAnswer( Element interpretation ) {
        
        StringBuilder buffer = new StringBuilder( "\"" ) ;
        Elements children = interpretation.children() ;
        
        for( Element child : children ) {
            if( !child.tagName().equalsIgnoreCase( "b" ) ) {
                buffer.append( child.text() );
            }
        }
        buffer.append( "\"" ) ;
        return WordUtils.wrap( buffer.toString(), 80 ) ;
    }
    
    public File[] getHTMLFiles() {
        File tmpDir = new File( "/home/sandeep/temp" ) ;
        return tmpDir.listFiles( new FileFilter() {
            public boolean accept( File pathname ) {
                return pathname.getName().endsWith( ".html" ) ;
            }
        } ) ;
    }
    
    public static void main( String[] args ) throws Exception {
        MoVInterpret driver = new MoVInterpret() ;
        driver.driveFromURL( 98, 104 ) ; 
    }
}
