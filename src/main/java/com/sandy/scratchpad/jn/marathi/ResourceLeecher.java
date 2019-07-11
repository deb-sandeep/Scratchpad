package com.sandy.scratchpad.jn.marathi;

import java.net.URL ;
import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.List ;

import org.apache.commons.lang.StringUtils ;
import org.apache.log4j.Logger ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.nodes.Node ;
import org.jsoup.nodes.TextNode ;
import org.jsoup.select.Elements ;

public class ResourceLeecher {

    private static final Logger log = Logger.getLogger( ResourceLeecher.class ) ;
    
//  "http://learn101.org/marathi_voc500.php",
    
    private static final String[] urls = {
          "http://learn101.org/marathi_nouns.php",
          "http://learn101.org/marathi_adjectives.php",
          "http://learn101.org/marathi_plural.php",
          "http://learn101.org/marathi_gender.php",
          "http://learn101.org/marathi_numbers.php",
          "http://learn101.org/marathi_phrases.php",
          "http://learn101.org/marathi_grammar.php",
          "http://learn101.org/marathi_vocabulary.php",
          "http://learn101.org/marathi_verbs.php"
    } ;
      
    private HashMap<String, List<MarathiFragment>> sections = null ;
    
    public ResourceLeecher( HashMap<String, List<MarathiFragment>> sections ) {
        this.sections = sections ;
    }
    
    public void processUrls() throws Exception {
        for( String url : urls ) {
            processURL( url ) ; 
        }
    }
    
    private void processURL( String urlStr ) throws Exception {
        
        log.debug( "Processing URL - " + urlStr ) ;
        URL url = new URL( urlStr ) ;
        Document doc = Jsoup.parse( url, 5000 ) ;
        Element body = doc.body() ;
        
        Elements tables = body.select( "[summary]" ) ;
        for( Element table : tables ) {
            processSection( table ) ;
        }
    }
    
    private void processSection( Element table ) {
        String summary = table.attr( "summary" ) ;
        
        List<MarathiFragment> sectionList = sections.get( summary ) ;
        List<MarathiFragment> fragmentList = new ArrayList<>() ; 

        Elements rows = table.select( "td" ) ;
        for( Element row : rows ) {
            MarathiFragment fragment = createFragment( summary, row ) ;
            fragmentList.add( fragment ) ;
        }
        
        if( sectionList == null ) {
            sectionList = new ArrayList<>() ;
            sections.put( summary, sectionList ) ;
        }
        sectionList.addAll( fragmentList ) ;
    }
    
    private MarathiFragment createFragment( String tableSummary, Element row ) {
        
        MarathiFragment fragment = new MarathiFragment() ;
        
        Elements englishText = row.getElementsByTag( "b" ) ;
        String capitalized = StringUtils.capitalize( englishText.text().trim() ) ;
        fragment.setEnglishText( capitalized ) ;
        
        Elements img = row.select( "img[class=ax]" ) ;
        if( !img.isEmpty() ) {
            fragment.setImgResourcePath( img.attr( "src" ) );
        }

        Elements pronunciation = row.select( "i" ) ;
        if( !pronunciation.isEmpty() ) {
            fragment.setPronunciation( pronunciation.text() ) ;
        }

        Elements audio = row.select( "audio" ) ;
        if( !audio.isEmpty() ) {
            fragment.setAudioResourcePath( audio.attr( "src" ) ) ;
        }
        
        for( Node node : row.childNodes() ) {
            if( node instanceof TextNode ) {
                String marathiText = node.toString().trim() ;
                if( !StringUtils.isEmpty( marathiText ) ) {
                    fragment.setMarathiText( marathiText ) ;
                }
            }
        }
        
        return fragment ;
    }
}
