package com.sandy.scratchpad.jn.englang.util;

import java.io.BufferedReader ;
import java.io.InputStream ;
import java.io.InputStreamReader ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;
import com.sandy.scratchpad.jn.englang.pronoun.PronounQAGenerator ;

import static java.lang.Character.isWhitespace ;

public class LineLoader {

    private static final Logger log = Logger.getLogger( LineLoader.class ) ;
    
    private static final int MIN_LINE_LEN = 40 ;
    private static final int MAX_LINE_LEN = 100 ;
    
    public List<String> loadLines( String... resourceNames ) 
        throws Exception {
        
        List<String> lines = new ArrayList<>() ;
        
        for( String resName : resourceNames ) {
            
            StringBuilder sb = new StringBuilder() ;
            
            InputStream is = PronounQAGenerator.class.getResourceAsStream( "/prose/" + resName ) ;
            BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) ;
            
            String line = null ;
            
            while( (line = reader.readLine()) != null ) {
                
                line = line.trim() ;
                
                if( StringUtil.isNotEmptyOrNull( line ) ) {
                
                    if( line.endsWith( "¬" ) ) {
                        line = line.substring( 0, line.length()-1 ) ;
                        sb.append( line ) ;
                    }
                    else {
                        sb.append( line + " " ) ;
                    }
                }
                else {
                    if( sb.length() != 0 ) {
                        lines.addAll( separateLines( sb.toString().trim() ) ) ;
                        sb = new StringBuilder() ;
                    }
                }
            }
            
            if( sb.length() != 0 ) {
                lines.addAll( separateLines( sb.toString().trim() ) ) ;
            }
        }

        for( String line : lines ) {
            log.debug( line ) ;
        }
        
        return lines ;
    }
    
    private List<String> separateLines( String input ) {
        
        StringBuilder sb = new StringBuilder( input ) ;
        List<String> lines = new ArrayList<>() ;
        
        int start = 0 ;
        int end = 0 ;
        
        end = getNextStop( sb, start ) ;
        
        while( end < sb.length() ) {
            
            String str = sb.substring( start, end+1 ).trim() ;
            str = str.replace( "  ", " " ) ;
            
            if( str.length() >= MIN_LINE_LEN && str.length() <= MAX_LINE_LEN ) {
                lines.add( str ) ;
            }
            
            start = end+1 ;
            end = getNextStop( sb, start ) ;
        }
        
        return lines ;
    }
    
    private int getNextStop( StringBuilder sb, int startIndex ) {

        if( startIndex >= sb.length() ) {
            return startIndex ;
        }
        
        int curIndex = eatWhitespaces( sb, startIndex ) ;
        
        char ch = sb.charAt( curIndex ) ;
        
        boolean withinQuotes = false ;
        
        while( ch != '.' ) {
            
            if( ch == '‘' || ch == '’' || ch == '\'' ) {
                if( curIndex < sb.length()-1 ) {
                    char nextChar = sb.charAt( curIndex+1 ) ;
                    if( nextChar >= 'a' && nextChar <= 'z' ) {
                        if( sb.charAt( curIndex-1 ) == ' ' ) {
                            // This is the ' in one's. Not ending quotes.
                            withinQuotes = !withinQuotes ;
                        }
                    }
                    else {
                        withinQuotes = !withinQuotes ;
                    }
                }
                else {
                    withinQuotes = !withinQuotes ;
                }
            }
            
            if( curIndex <= sb.length()-4 ) {
                if( sb.subSequence( curIndex+1, curIndex+4 )
                      .equals( "..." ) ) {
                    curIndex = curIndex+3 ;
                    ch = sb.charAt( curIndex ) ;
                }
            }
            
            curIndex++ ;
            if( curIndex >= sb.length()-1 ) {
                curIndex = sb.length()-1 ;
                break ;
            }
            
            ch = sb.charAt( curIndex ) ;
            if( withinQuotes && ch == '.' ) {
                curIndex++ ;
                ch = sb.charAt( curIndex ) ;
            }
        }
        
        return curIndex ;
    }
    
    private int eatWhitespaces( StringBuilder sb, int startIndex ) {
        
        int curIndex = startIndex ;
        
        char ch = sb.charAt( curIndex ) ;
        
        while( curIndex < sb.length()-1 && isWhitespace( ch ) ) {
            curIndex++ ;
            if( curIndex < sb.length() ) {
                ch = sb.charAt( curIndex ) ;
            }
        }
        
        return curIndex ;
    }
    
    public void filterLines( String resName ) 
            throws Exception {
            
        List<String> lines = new ArrayList<>() ;
        
        InputStream is = PronounQAGenerator.class.getResourceAsStream( "/prose/" + resName ) ;
        BufferedReader reader = new BufferedReader( new InputStreamReader( is ) ) ;
        
        String line = null ;
        
        while( (line = reader.readLine()) != null ) {
            
            line = line.trim() ;
            
            if( StringUtil.isNotEmptyOrNull( line ) ) {
            
                if( line.length() >= MIN_LINE_LEN && 
                    line.length() <= MAX_LINE_LEN ) {
                    lines.add( line ) ;
                }
            }
        }
        
        for( String l : lines ) {
            log.debug( l ) ;
        }
    }
        
    public static void main( String[] args ) throws Exception {
        
        //new LineLoader().loadLines( "harry-potter-philosophers-stone.txt" ) ;
        //new LineLoader().loadLines( "prose.txt" ) ;
        //new LineLoader().filterLines( "pronoun-sentences.txt" ) ;
    }
}
