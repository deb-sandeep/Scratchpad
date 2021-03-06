package com.sandy.scratchpad.jn.exercise;

import java.util.ArrayList ;
import java.util.Collections ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;
import java.util.regex.Matcher ;
import java.util.regex.Pattern ;

import org.apache.commons.lang.StringUtils ;
import org.apache.log4j.Logger ;

public class QuestionManager {
    
    private static Logger log = Logger.getLogger( QuestionManager.class ) ;
    private static Pattern pattern = Pattern.compile( "([0-9]+(\\.[0-9]+)*)(\\(([0-9]+)\\))?(Ans|Hdr)?" ) ;
    
    private List<ImgMeta> metaList = new ArrayList<ImgMeta>() ;
    
    private Map<String, Exercise> exerciseMap = new LinkedHashMap<String, Exercise>() ;
    
    private String bookName = null ;
    private String chapterName = null ;
    
    public QuestionManager( String bookName, String chapterName ) {
        this.bookName = bookName ;
        this.chapterName = chapterName ;
    }
    
    public void buildImageMeta( String imageName ) {
        
        log.debug( "Building image meta for " + imageName );
        ImgMeta m = new ImgMeta() ;
        if( imageName.startsWith( "ex_" ) ) {
            m.setExample( true ) ;
            parseAndPopulateExerciseImageName( imageName, m ) ;
        }
        else if( imageName.startsWith( "Ex" ) ) {
            parseAndPopulateQuestionImageName( imageName, m ) ;
        }
        
        metaList.add( m ) ;
        Collections.sort( metaList );
    }
    
    public Map<String, Exercise> createQuestions() {
        
        for( ImgMeta meta : metaList ) {
            
            String exerciseName = null ;
            exerciseName = (meta.isExample()) ? "ex_" : meta.getExerciseName() ; 
            
            Exercise exercise = getExercise( exerciseName ) ;
            exercise.buildQuestion( meta ) ; 
        }
        
        return this.exerciseMap ;
    }
    
    private Exercise getExercise( String eId ) {
        
        Exercise ex = exerciseMap.get( eId ) ;
        if( ex == null ) {
            ex = new Exercise( bookName, chapterName, eId ) ;
            exerciseMap.put( eId, ex ) ;
        }
        return ex ;
    }
    
    public List<ImgMeta> getImgMetaList() {
        return this.metaList ;
    }
    
    private void parseAndPopulateExerciseImageName( String imgName, ImgMeta m ) {
        
        String name = StringUtils.substringAfter( imgName, "ex_" ) ;
        populateSequenceAttributes( name, m ) ;
    }
    
    private void parseAndPopulateQuestionImageName( String imgName, ImgMeta m ) {
        
        String[] nameParts = imgName.split( "_" ) ;
        String exName = StringUtils.substringAfter( nameParts[0], "Ex" ) ; 
        m.setExerciseName( exName ) ;
        
        populateSequenceAttributes( nameParts[1], m ) ;
    }
    
    private void populateSequenceAttributes( String seqPart, ImgMeta m ) {
        
        Matcher matcher = pattern.matcher( seqPart ) ;
        if( matcher.matches() ) {
            if( matcher.group( 5 ) != null ) {
                if( matcher.group( 5 ).equals( "Ans" ) ) {
                    m.setAnswer( true ) ;
                }
                else if( matcher.group(5).equals( "Hdr" ) ) {
                    m.setHeader( true ) ;
                }
            }
            
            if( matcher.group( 4 ) != null ) {
                m.setPartNumber( Integer.parseInt( matcher.group( 4 ) ) ) ;
            }
            
            String[] seqParts  = matcher.group( 1 ).split( "\\." ) ;
            int[]    qSeqParts = new int[seqParts.length] ;
            
            for( int i=0; i<seqParts.length; i++ ) {
                qSeqParts[i] = Integer.parseInt( seqParts[i] ) ;
            }
            
            m.setSequenceParts( qSeqParts ) ;
        }
        else {
            throw new IllegalArgumentException( 
                 "Image " + seqPart + " does not match established pattern." ) ;
        }
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
