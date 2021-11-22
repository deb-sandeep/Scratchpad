package com.sandy.scratchpad.jn.paryayvachi;

import java.io.File ;
import java.io.FileNotFoundException ;
import java.util.ArrayList ;
import java.util.Collections ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;
import java.util.concurrent.ThreadLocalRandom ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class ParyayvachiMCQCreator {

    private static final Logger log = Logger.getLogger( ParyayvachiMCQCreator.class ) ;
    
    public static void main( String[] args ) throws Exception {
        ParyayvachiMCQCreator app = new ParyayvachiMCQCreator() ;
        app.loadData() ;
        app.createDataStructures() ;
        app.generateQuestions() ;
    }
    
    private Map<String, List<String>> rawData = new HashMap<>() ;
    private List<MCQ> mcqs = new ArrayList<>() ;
    private List<String> allOptions = new ArrayList<>() ;
    
    private void loadData() throws Exception {
        
        File file = getDataFile() ;
        List<String> lines = FileUtils.readLines( file ) ;
        
        for( String line : lines ) {
            if( StringUtil.isNotEmptyOrNull( line ) ) {
                
                line = line.trim() ;
                
                if( line.contains( "=" ) ) {
                    String word = null ;
                    String[] meanings = null ;
                    String[] parts = line.split( "=" ) ;
                    
                    word = parts[0].trim() ;
                    meanings = parts[1].split( "," ) ;
                    
                    List<String> meaningList = new ArrayList<>() ;
                    for( String meaning : meanings ) {
                        meaningList.add( meaning.trim() ) ;
                        allOptions.add( meaning.trim() ) ;
                    }
                    
                    rawData.put( word, meaningList ) ;
                }
            }
        }
    }
    
    public static File getDataFile() throws Exception {
        
        File userHomeDir = new File( System.getProperty("user.home") ) ;
        File file = new File( userHomeDir, "paryayvachi.txt" ) ;
        if( !file.exists() ) {
            throw new FileNotFoundException() ;
        }
        return file ;
    }

    private void createDataStructures() {
        for( String key : rawData.keySet() ) {
            List<String> meanings = rawData.get( key ) ;
            
            for( String meaning : meanings ) {
                MCQ mcq = new MCQ( key ) ;
                mcq.addOption( new MCQOption( meaning, true ) ) ;
                
                addRandomWrongOptions( mcq, meanings ) ;
                
                mcqs.add( mcq ) ;
            }
        }
    }
    
    private void addRandomWrongOptions( MCQ mcq, List<String> meanings ) {
        while( mcq.getNumOptions() < 4 ) {
            int randomIdx = ThreadLocalRandom.current().nextInt( 0, allOptions.size() ) ;
            String randomAnswer = allOptions.get( randomIdx ) ;
            if( !meanings.contains( randomAnswer ) ) {
                if( !mcq.containsOption( randomAnswer ) ) {
                    mcq.addOption( new MCQOption( randomAnswer ) ) ;
                }
            }
        }
    }
    
    private void generateQuestions() {
        Collections.shuffle( mcqs ) ;
        for( MCQ mcq : mcqs ) {
            log.debug( mcq ) ;
            log.debug( "" ) ;
        }
    }
}
