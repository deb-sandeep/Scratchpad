package com.sandy.scratchpad.jee.ie;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;

public class IEScraper {
    private static final Logger log = Logger.getLogger( IEScraper.class ) ;
    
    public static void main( String[] args ) throws Exception {
        String   contents = FileUtils.readFileToString( new File( "/Users/sandeep/temp/ie.html" ) ) ;
        Document doc      = Jsoup.parse( contents ) ;
        new IEScraper().process( doc ) ;
    }
    
    public void process( Document doc ) {
        StringBuilder sb = new StringBuilder( "[\n") ;
        Elements interpretRows = doc.select( "table.wikitable>tbody tr" ) ;
        for( Element row : interpretRows ) {
            sb.append( "\t" + getElementRow( row ) + ",\n" ) ;
        }
        sb.delete( sb.length()-2, sb.length()-1 ) ;
        sb.append( "]" ) ;
        log.debug( sb.toString() ) ;
    }
    
    private String getElementRow( Element row ) {
        StringBuilder sb = new StringBuilder( "{" ) ;
        for( int i=0; i<row.children().size(); i++ ) {
            Element cell = row.child( i ) ;
            if( i == 0 ) {
                sb.append( "Z:" + cell.text() + ", " ) ;
            }
            else if( i>2 ){
                String text = cell.text() ;
                if( !text.isEmpty() ) {
                    double ie = Double.parseDouble( text.replaceAll( ",", "" ) ) ;
                    sb.append( "IE" + (i-2) + ":" + (int)Math.round( ie ) + ", " ) ;
                }
            }
        }
        if( row.children().size() < 13 ) {
            for( int i=row.children().size()-2; i<=10; i++ ) {
                sb.append( "IE" + i + ":null, " ) ;
            }
        }
        sb.delete( sb.length()-2, sb.length() ) ;
        sb.append( "}" ) ;
        return sb.toString() ;
    }
}
