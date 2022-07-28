package com.sandy.scratchpad.jn.englang.preposition;

import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.scratchpad.jn.englang.preposition.Line.PrepositionSegment ;

public class QGenHelper {
    
    private static final Logger log = Logger.getLogger( QGenHelper.class ) ;
    
    private String lineStr = null ;
    private List<PrepositionSegment> segments = null ;
    
    public QGenHelper( Line line ) {
        this.lineStr = line.toString() ;
        this.segments = line.getPrepositionSegments() ;
    }
    
    public String segmentString( PrepositionSegment seg ) {
        return lineStr.substring( seg.startIndex, seg.endIndex ) ;
    }

    public void generateFIB() {
        
        StringBuilder qBuffer = new StringBuilder() ;
        StringBuilder aBuffer = new StringBuilder() ;
        
        qBuffer.append( "@fib \"" ) ;
        
        int startIndex = 0 ;
        int fibIndex = 0 ;
        
        for( PrepositionSegment seg : segments ) {
            
            qBuffer.append( lineStr.substring( startIndex, seg.startIndex ) ) ;
            qBuffer.append( "{" + fibIndex + "}" ) ;
            aBuffer.append( "\"" + segmentString( seg ) + "\"\n" ) ;
            
            startIndex = seg.endIndex ;
            fibIndex++ ;
        }
        
        if( startIndex < lineStr.length() ) {
            qBuffer.append( lineStr.substring( startIndex ) ) ;
        }
        
        qBuffer.append( "\"" ) ;
        
        log.info( qBuffer ) ;
        log.info( aBuffer ) ;
    }
}
