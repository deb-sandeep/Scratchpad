package com.sandy.scratchpad.handwriting;

import java.io.InputStream ;
import java.util.ArrayList ;
import java.util.Arrays ;
import java.util.Comparator ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;
import java.util.Map.Entry ;
import java.util.Set ;
import java.util.TreeSet ;

import org.apache.commons.io.IOUtils ;
import org.apache.log4j.Logger ;

import com.sandy.scratchpad.handwriting.Word.ScoreType ;

public class DrillCreator {

    static final Logger log = Logger.getLogger( DrillCreator.class ) ;
    
    private String resourceName = null ;
    
    private Map<ScoreType, List<Word>> scoredWordMap = 
                                     new HashMap<Word.ScoreType, List<Word>>() ;
    
    public DrillCreator( String resName ) throws Exception {
        this.resourceName = resName ;
        
        scoredWordMap.put( ScoreType.ONE_PLUS            , new ArrayList<Word>() ) ;
        scoredWordMap.put( ScoreType.ONE_PLUS_O          , new ArrayList<Word>() ) ;
        scoredWordMap.put( ScoreType.ONE_PLUS_I          , new ArrayList<Word>() ) ;
        scoredWordMap.put( ScoreType.TWO_PLUS            , new ArrayList<Word>() ) ;
        scoredWordMap.put( ScoreType.THREE_PLUS          , new ArrayList<Word>() ) ;
        scoredWordMap.put( ScoreType.TWO_MINUS           , new ArrayList<Word>() ) ;
        scoredWordMap.put( ScoreType.THREE_PLUS_TWO_MINUS, new ArrayList<Word>() ) ;
        scoredWordMap.put( ScoreType.ONE_MINUS           , new ArrayList<Word>() ) ;
        
        loadWords() ;
    }
    
    private void loadWords() throws Exception {
        
        InputStream is = null ;
        
        is = DrillCreator.class.getResourceAsStream( resourceName ) ;
        List<String> lines = IOUtils.readLines( is ) ;
        
        for( String line : lines ) {
            String[] rawWords = line.split( "\\s+" ) ;
            for( String word : rawWords ) {
                Word w = new Word( word.trim() ) ;
                for( List<Word> wordList : scoredWordMap.values() ) {
                    wordList.add( w ) ;
                }
            }
        }
        
        for( Entry<ScoreType, List<Word>> entry : scoredWordMap.entrySet() ) {
            entry.getValue().sort( new Comparator<Word>() {
                @Override
                public int compare( Word w1, Word w2 ) {
                    int scoreW1 = w1.getScore( entry.getKey() ) ;
                    int scoreW2 = w2.getScore( entry.getKey() ) ;
                    return scoreW2 - scoreW1 ;
                }
            } );
        }
    }
    
    public void createDrill( int maxNumBatches, ScoreType...  acceptedType ) throws Exception {
        
        boolean keepRunning = true ;
        int batchNum = 1 ;
        
        List<ScoreType> validTypes = Arrays.asList( ScoreType.values() ) ;
        if( acceptedType != null && acceptedType.length > 0 ) {
            validTypes = new ArrayList<>() ;
            validTypes.addAll( Arrays.asList( acceptedType ) ) ;
        }
        
        while( keepRunning ) {
            
            int numWords = 0 ;
            Set<String> selectedWords = new TreeSet<>() ;
            
            while( numWords < 44 && keepRunning ) {
                
                keepRunning = false ;
                
                for( Entry<ScoreType, List<Word>> entry : scoredWordMap.entrySet() ) {
                    if( validTypes.contains( entry.getKey()  ) ) {
                        if( !entry.getValue().isEmpty() ) {
                            keepRunning = true ;
                            Word word = entry.getValue().remove( 0 ) ;
                            int score = word.getScore( entry.getKey() ) ;
                            
                            if( score > 2 ) {
                                selectedWords.add( word.getWord() ) ;
                                numWords++ ;
                                if( numWords == 44 ) break ;
                            }
                        }
                    }
                }
            }
            
            if( !selectedWords.isEmpty() ) {
                log.debug( "--------------------------------------------------" ) ;
                String[] selWords = selectedWords.toArray( new String[]{} ) ;
                for( int i=0; i<selWords.length; i+=2 ) {
                    String word1 = selWords[i] ;
                    String word2 = ( i <selWords.length-1 ) ? selWords[i+1] : "" ;
                    log.debug( word1 + "  " + word2 ) ;
                }
                
                if( batchNum == maxNumBatches ) {
                    break ;
                }
            }
        }
    }
    
    public static void main( String[] args ) throws Exception {
        DrillCreator drillCreator = new DrillCreator( "/15charwords.txt" ) ;
        drillCreator.createDrill( 1, ScoreType.ONE_PLUS_O ) ;
    }
}
