package com.sandy.scratchpad.jn.englang.scrape;

import java.net.URL ;
import java.text.SimpleDateFormat ;
import java.util.ArrayList ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.log4j.Logger ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.select.Elements ;

public class GrammarExerciseTOC {
    
    private static final Logger log = Logger.getLogger( GrammarExerciseTOC.class ) ;
    public  static SimpleDateFormat SDF = new SimpleDateFormat( "dd/MM/yy" ) ;
    
    private Map<String, ExerciseMetaData> exerciseMap = new LinkedHashMap<String, ExerciseMetaData>() ;

    public GrammarExerciseTOC() throws Exception {
        loadTOC() ;
    }
    
    private void loadTOC() throws Exception {
        
        log.debug( "Loading grammar exercise TOC" ) ;
        URL url = new URL( "http://www.englishgrammar.org/lessons/" ) ;
        
        log.debug( "Parsing - " + url.toExternalForm() ) ;
        Document doc = Jsoup.parse( url, 10000 ) ;
        
        log.debug( "Page loaded.. trying to parse" ) ;
        Elements exerciseList = doc.body().select( "main.content ul li" ) ;
        
        log.debug( "Adding exercise meta data" ) ;
        for( Element row : exerciseList ) {
            processExerciseMeta( row ) ;
        }
    }

    private void processExerciseMeta( Element row ) 
        throws Exception {
        
        String  dateText = row.ownText() ;
        Element link     = row.getElementsByAttributeValue( "target", "_blank" ).first() ;
        String  descr    = link.text() ;
        
        ExerciseMetaData meta = new ExerciseMetaData() ;
        meta.setPublishDate( SDF.parse( dateText ) ) ;
        meta.setDescription( descr ) ;
        meta.setUrl( new URL( link.attr( "href" ) ) );
        
        exerciseMap.put( dateText + descr, meta ) ;
        log.debug( "\t" + meta ) ;
    }
    
    public List<ExerciseMetaData> getExercises( List<String> titleContents ) {
        
        List<ExerciseMetaData> results = new ArrayList<ExerciseMetaData>() ;
        
        for( String title : exerciseMap.keySet() ) {
            for( String part : titleContents ) {
                if( title.toLowerCase().contains( part ) ) {
                    results.add( exerciseMap.get( title ) ) ;
                    break ;
                }
            }
        }
        
        return results ;
    }
}
