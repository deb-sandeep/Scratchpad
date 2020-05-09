package com.sandy.scratchpad.yg20;

import org.apache.commons.lang.StringUtils ;

public class QMeta {
    
    public String sub = null ;
    public int qNo = -1 ;
    public String qType = null ;
    public String ans = null ;

    public QMeta( String sub, int qNo, String qType, String ans ) {
        this.sub = sub ;
        this.qNo = qNo ;
        this.qType = qType ;
        this.ans = ans ;
        
        if( qType.equals( "MCA" ) ) {
            String newVal = "" ;
            for( int i=0; i<ans.length()-1; i++ ) {
                newVal += ans.charAt( i ) + "," ;
            }
            newVal += ans.charAt( ans.length()-1 ) ;
            this.ans = newVal ;
        }
    }
    
    public String toString() {
        StringBuffer buffer = new StringBuffer() ;
        buffer.append( StringUtils.rightPad( sub, 4 ) )
              .append( " | " )
              .append( StringUtils.rightPad( "" + qNo, 3 ) )
              .append( " | " )
              .append( StringUtils.rightPad( qType, 3 ) )
              .append( " | " )
              .append( StringUtils.rightPad( ans, 4 ) ) ;
        return buffer.toString() ;
    }
}
