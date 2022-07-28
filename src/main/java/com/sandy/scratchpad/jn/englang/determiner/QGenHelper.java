package com.sandy.scratchpad.jn.englang.determiner;

import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.scratchpad.jn.englang.determiner.Line.Segment ;

public class QGenHelper {
    
    private static final Logger log = Logger.getLogger( QGenHelper.class ) ;
    
    private String lineStr = null ;
    private List<Segment> segments = null ;
    
    public QGenHelper( Line line ) {
        this.lineStr = line.toString() ;
        this.segments = line.getPronouns() ;
    }
    
    private String segmentString( Segment seg ) {
        return lineStr.substring( seg.startIndex, seg.endIndex ) ;
    }

    public void generateIdentifyQuestion( DeterminerType type ) {
        
        StringBuilder sb = new StringBuilder() ;
        sb.append( "@qa \"Identify **" + type.getName() + "** determiners:\n\n" )
          .append( lineStr + "\"\n" )
          .append( "\"" ) ;
        
        int startIndex = 0 ;
        
        for( Segment seg : segments ) {
            
            if( seg.determiner.getType() == type ) {
                
                sb.append( lineStr.substring( startIndex, seg.startIndex ) ) ;
                sb.append( "{{@red **" + segmentString( seg ) + "**}}" ) ;
                startIndex = seg.endIndex ;
            }
        }
        
        if( startIndex < lineStr.length() ) {
            sb.append( lineStr.substring( startIndex ) ) ;
        }
        
        sb.append( "\"" ) ;
        
        log.info( sb ) ;
    }
    
    public void generateFIB() {
        
        StringBuilder qBuffer = new StringBuilder() ;
        StringBuilder aBuffer = new StringBuilder() ;
        
        qBuffer.append( "\n@fib \"" ) ;
        
        int startIndex = 0 ;
        int blankIndex = 0 ;
        
        for( Segment seg : segments ) {
            
            String type = seg.determiner.getType().getName() ;
            
            qBuffer.append( lineStr.substring( startIndex, seg.startIndex ) ) ;
            qBuffer.append( "{" + blankIndex + "} _(" + type + ")_" ) ;
            
            startIndex = seg.endIndex ;
            blankIndex++ ;
            
            aBuffer.append( "\"" + segmentString( seg ) + "\"\n" ) ;
        }
        
        if( startIndex < lineStr.length() ) {
            qBuffer.append( lineStr.substring( startIndex ) ) ;
        }
        
        qBuffer.append( "\"" ) ;
        
        log.info( qBuffer ) ;
        log.info( aBuffer ) ;
    }
}
