package com.sandy.scratchpad.jn.englang.pronoun;

import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.scratchpad.jn.englang.pronoun.Line.Segment ;

public class PronounQGen {
    
    private static final Logger log = Logger.getLogger( PronounQGen.class ) ;
    
    private String lineStr = null ;
    private List<Segment> segments = null ;
    
    public PronounQGen( Line line ) {
        this.lineStr = line.toString() ;
        this.segments = line.getPronouns() ;
    }
    
    private String segmentString( Segment seg ) {
        return lineStr.substring( seg.startIndex, seg.endIndex ) ;
    }

    public void generateIdentifyQuestion( PronounType type ) {
        
        StringBuilder sb = new StringBuilder() ;
        sb.append( "\n@qa \"Identify **" + type.getName() + "** pronouns:\n\n" )
          .append( lineStr + "\"\n" )
          .append( "\"" ) ;
        
        
        int startIndex = 0 ;
        
        for( Segment seg : segments ) {
            
            if( seg.pronoun.getType() == type ) {
                
                sb.append( lineStr.substring( startIndex, seg.startIndex ) ) ;
                sb.append( "{{@red **" + segmentString( seg ) + "**}}" ) ;
                startIndex = seg.endIndex ;
            }
        }
        
        if( startIndex < lineStr.length() ) {
            sb.append( lineStr.substring( startIndex ) ) ;
        }
        
        sb.append( "\"" ) ;
        
        log.debug( sb ) ;
    }
}
