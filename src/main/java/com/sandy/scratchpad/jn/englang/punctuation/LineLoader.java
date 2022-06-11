package com.sandy.scratchpad.jn.englang.punctuation;

import java.io.File ;
import java.util.ArrayList ;
import java.util.Iterator ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class LineLoader {
    
    static final Logger log = Logger.getLogger( LineLoader.class ) ;
    
    public List<Line> loadLines() throws Exception {
        File file = new File( "/Users/sandeep/temp/primordia.txt" ) ;
        List<String> lines = FileUtils.readLines( file ) ;
        List<Line> lineObjs = new ArrayList<>() ;
        
        for( Iterator<String> iter = lines.iterator(); iter.hasNext(); ) {
            String line = iter.next().trim() ;
            if( StringUtil.isEmptyOrNull( line ) ) {
                iter.remove() ;
                continue ;
            }
            
            if( !lineEndsInPunctuation( line ) ) {
                iter.remove() ;
                continue ;
            }
            
            lineObjs.addAll( createLines( line ) ) ;
        }
        return lineObjs ;
    }
    
    private boolean lineEndsInPunctuation( String line ) {
        
        for( char punctuation : PunctuationGen.PUNCTUATIONS ) {
            if( line.charAt( line.length()-1 ) == punctuation ) {
                return true ;
            }
        }
        return false ;
    }
    
    private List<Line> createLines( String line ) {
        
        List<Line> lineObjs = new ArrayList<>() ;
        
        StringBuilder sb = new StringBuilder() ;
        boolean firstChar = false ;
        boolean withinQuotes = false ;
        
        for( int i=0; i<line.length(); i++ ) {
            char ch = line.charAt( i ) ;
            
            if( firstChar && Character.isWhitespace( ch ) ) {
                continue ;
            }
            
            sb.append( ch ) ;
            if( firstChar ) {
                firstChar = false ;
            }
            
            if( ch == '"' ) {
                withinQuotes = !withinQuotes ;
            }
            
            if( !withinQuotes ) {
                if( ch == '.' || ch == '?' || ch == '!' ) {
                    lineObjs.add( new Line( sb.toString().trim() ) ) ;
                    sb = new StringBuilder() ;
                    firstChar = true ;
                }
            }
        }
        
        return lineObjs ;
    }
}
