package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.Collections ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;
import java.util.regex.Matcher ;
import java.util.regex.Pattern ;

import org.apache.log4j.Logger ;

public class QuestionManager {
    
    private static Logger log = Logger.getLogger( QuestionManager.class ) ;
    private static Pattern pattern = Pattern.compile( "([0-9]+(\\.[0-9]+)*)(\\(([0-9]+)\\))?(Ans|Hdr)?" ) ;
    
    private List<ImgMeta> metaList = new ArrayList<ImgMeta>() ;
    
    private Map<String, Exercise> exerciseMap = new LinkedHashMap<String, Exercise>() ;
    
    private String bookName = null ;
    
    public QuestionManager( String bookName ) {
        this.bookName = bookName ;
    }
    
    public void buildImageMeta( String imageName ) {
        ImgMeta m = new ImgMeta( imageName ) ;
        metaList.add( m ) ;
        Collections.sort( metaList );
    }
    
    public void printImgMetaList() {
        for( ImgMeta meta : metaList ) {
            log.debug( meta.getFileName() ) ;
        }
    }
    
    public Map<String, Exercise> createExercises() {
        
        for( ImgMeta meta : metaList ) {
            log.debug( "Building question - " + meta ) ;
            String exerciseName = meta.getExerciseName() ;
            Exercise exercise = getExercise( exerciseName ) ;
            try {
                exercise.buildQuestion( meta ) ;
            }
            catch( Exception e ) {
                log.error( "Unanticipated exception.", e ) ;
                log.error( meta ) ;
                throw new RuntimeException(e);
            }
        }
        return this.exerciseMap ;
    }
    
    private Exercise getExercise( String eId ) {
        
        Exercise ex = exerciseMap.get( eId ) ;
        if( ex == null ) {
            ex = new Exercise( bookName, eId ) ;
            exerciseMap.put( eId, ex ) ;
        }
        return ex ;
    }
    
    public List<ImgMeta> getImgMetaList() {
        return this.metaList ;
    }
    
    public static void main( String[] args ) throws Exception {
        
        Matcher matcher = pattern.matcher( "1.2(23)Hdr" ) ;
        
        if( matcher.matches() ) {
            for( int i=0; i<=matcher.groupCount(); i++ ) {
                log.debug( "Group " + i + " = " + matcher.group( i ) ) ;
            }
        }
    }
}
