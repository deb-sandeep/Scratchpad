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

public class OneToManyMCQCreator {

    private static final Logger log = Logger.getLogger( OneToManyMCQCreator.class ) ;
    
    private static final int MAX_OPTIONS = 6 ;
    
    public static void main( String[] args ) throws Exception {
        OneToManyMCQCreator app = new OneToManyMCQCreator() ;
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
                    
                    String keyword = null ;
                    String[] values = null ;
                    String[] parts = line.split( "=" ) ;
                    
                    keyword = parts[0].trim() ;
                    values = parts[1].split( "," ) ;
                    
                    List<String> valueList = new ArrayList<>() ;
                    for( String value : values ) {
                        valueList.add( value.trim() ) ;
                        allOptions.add( value.trim() ) ;
                    }
                    
                    rawData.put( keyword, valueList ) ;
                }
            }
        }
    }
    
    public static File getDataFile() throws Exception {
        
        File userHomeDir = new File( System.getProperty("user.home") ) ;
        File file = new File( userHomeDir, "one-to-many.txt" ) ;
        if( !file.exists() ) {
            throw new FileNotFoundException() ;
        }
        return file ;
    }

    private void createDataStructures() {
        
        
        List<String> keywords = new ArrayList<String>( rawData.keySet() ) ;
        
        for( String keyword : rawData.keySet() ) {
            
            List<String> values = rawData.get( keyword ) ;
            
            for( String value : values ) {
                
                MCQ mcq = new MCQ( createKeywordToValuesQuestion( keyword ) ) ;
                mcq.addOption( new MCQOption( value, true ) ) ;
                addRandomWrongValuesOptions( mcq, values ) ;                
                mcqs.add( mcq ) ;
                
                mcq = new MCQ( createValueToKeywordsQuestion( value ) ) ;
                mcq.addOption( new MCQOption( keyword, true ) ) ;
                addRandomWrongKeywordOptions( mcq, keywords ) ;                
                mcqs.add( mcq ) ;
                
            }
        }
    }
    
    private String createKeywordToValuesQuestion( String keyword ) {
        
        return "Identify **" + keyword + "** pronouns."  ;
    }
    
    private String createValueToKeywordsQuestion( String keyword ) {
        
        return "Identify the pronoun type for - **" + keyword + "**"  ;
    }
    
    private void addRandomWrongValuesOptions( MCQ mcq, List<String> meanings ) {
        
        while( mcq.getNumOptions() < MAX_OPTIONS ) {
            
            int randomIdx = ThreadLocalRandom.current().nextInt( 0, allOptions.size() ) ;
            String randomAnswer = allOptions.get( randomIdx ) ;
            
            if( !meanings.contains( randomAnswer ) ) {
                if( !mcq.containsOption( randomAnswer ) ) {
                    mcq.addOption( new MCQOption( randomAnswer ) ) ;
                }
            }
        }
    }
    
    private void addRandomWrongKeywordOptions( MCQ mcq, List<String> keywords ) {
        
        while( mcq.getNumOptions() < MAX_OPTIONS ) {
            
            int randomIdx = ThreadLocalRandom.current().nextInt( 0, keywords.size() ) ;
            String randomAnswer = keywords.get( randomIdx ) ;
            
            if( !mcq.containsOption( randomAnswer ) ) {
                mcq.addOption( new MCQOption( randomAnswer ) ) ;
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
