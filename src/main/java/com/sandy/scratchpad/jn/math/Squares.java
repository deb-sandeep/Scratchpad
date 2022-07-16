package com.sandy.scratchpad.jn.math;

import java.util.ArrayList ;
import java.util.Collections ;
import java.util.List ;

import org.apache.log4j.Logger ;

public class Squares {

    private static final Logger log = Logger.getLogger( Squares.class ) ;
    
    public void generateQuestions() {
        for( int i=11; i<100; i++ ) {
            generateSCA( "Square", i, i*i ) ;
            generateSCA( "Square root", i*i, i ) ;
        }
        
        //for( int i=11; i<100; i++ ) {
        //    log.debug( "@fib \"Square of **" + i + "** is {0}.\" \"" + i*i + "\"" );
        //    log.debug( "@fib \"Square root of **" + (i*i) + "** is {0}.\" \"" + i + "\"" );
        //}
    }
    
    private void generateSCA( String what, int i, int square ) {

        List<Integer> options = options( square ) ;
        
        log.debug( "@multi_choice \"" + what + " of **" + i + "** is:\" {" ) ;
        log.debug( "    @options {" ) ;
        for( int j=0; j<options.size(); j++ ) {
            StringBuilder sb = new StringBuilder() ;
            sb.append( "      \"" + options.get( j ) + "\"" ) ;
            if( options.get(j) == square ) {
                sb.append( " correct" ) ;
            }
            
            if( j < options.size()-1 ) {
                sb.append( "," ) ;
            }
            
            log.debug( sb ) ;
        }
        log.debug( "    }" ) ;
        log.debug( "}\n" ) ;
    }
    
    private List<Integer> options( int square ) {
        
        List<Integer> options = new ArrayList<>() ;
        
        char[] chars = Integer.toString( square ).toCharArray() ;
        
        options.add( square ) ;
        while( options.size() < 4 ) {
            for( int i=0; i<chars.length-1; i++ ) {
                if( Math.random() <= 0.5 ) {
                    int randomInt = (int)(Math.random()*10) ;
                    while( i == 0 && randomInt == 0 ) {
                        randomInt = (int)(Math.random()*10) ;
                    }
                    chars[i] = Integer.toString( randomInt ).charAt( 0 ) ;
                }
            }
            
            int val = Integer.parseInt( new String( chars ) ) ;
            if( !options.contains( val ) ) {
                options.add( val ) ;
            }
        }
        Collections.shuffle( options ) ;
        return options ;
    }
    
    public static void main( String[] args ) {
        new Squares().generateQuestions() ;
    }
}
