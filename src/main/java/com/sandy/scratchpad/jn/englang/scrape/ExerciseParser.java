package com.sandy.scratchpad.jn.englang.scrape;

import java.net.URL ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.log4j.Logger ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.select.Elements ;

public class ExerciseParser {

    private static Logger log = Logger.getLogger( ExerciseParser.class ) ;
    
    private ExerciseMetaData meta = null ;
    private List<String> contentStringList = null ;
    
    public ExerciseParser( ExerciseMetaData meta ) {
        this.meta = meta ;
    }
    
    public ExerciseMetaData getMeta() {
        return this.meta ;
    }
    
    public String extractQuestions() throws Exception {
        
        contentStringList = new ArrayList<>() ;
        
        log.debug( "Loading questions from " + meta ) ;
        Document doc = Jsoup.parse( meta.getUrl(), 10000 ) ;
        
        log.debug( "Page loaded.. trying to parse" ) ;
        Elements pList = doc.body().select( "div.entry-content p, div.entry-content h3" ) ;
        
        log.debug( "Processing questions" ) ;
        boolean hasQuestions = false ; 
        
        for( Element para : pList ) {
            String paraText = para.text() ;
            contentStringList.add( paraText ) ;
            
            if( !hasQuestions && paraText.equals( "Answers" ) ) {
                hasQuestions = true ;
            }
        }
        
        if( hasQuestions ) {
            ExerciseContentProcessor processor = null ;
            processor = new ExerciseContentProcessor( meta, contentStringList ) ;
            processor.process() ;
            
            String questionBank = processor.getQuestionBank() ;
            return questionBank ;
        }
        else {
            log.info( "No questions found." ) ;
        }
        return null ;
    }
    
    public static void main( String[] args ) throws Exception {
        
        ExerciseMetaData meta = new ExerciseMetaData() ;
        meta.setDescription( "Adjective or adverb exercise" ) ;
        meta.setPublishDate( GrammarExerciseTOC.SDF.parse( "07/04/12" ) );
        meta.setUrl( new URL( "http://www.englishgrammar.org/adjective-adverb-exercise/" ) ) ;
        
        ExerciseParser parser = new ExerciseParser( meta ) ;
        String questions = parser.extractQuestions() ;
        log.debug( questions ) ;
    }
}
