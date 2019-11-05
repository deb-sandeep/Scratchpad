package com.sandy.scratchpad.sconsole.lapreconstruct;

import java.io.InputStream ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.scratchpad.xlsutil.XLSRow ;
import com.sandy.scratchpad.xlsutil.XLSRowFilter.AND ;
import com.sandy.scratchpad.xlsutil.XLSUtil ;
import com.sandy.scratchpad.xlsutil.XLSWrapper ;
import com.sandy.scratchpad.xlsutil.filter.NumFilter ;
import com.sandy.scratchpad.xlsutil.filter.StrFilter ;

public class XLSWrapperTest {

    static Logger log = Logger.getLogger( XLSWrapperTest.class ) ;
    
    private static String resName = "/LapRepair.xlsx" ;
    
    public void execute1() throws Exception {
        InputStream is = XLSWrapperTest.class.getResourceAsStream( resName ) ;
        XLSWrapper xls = new XLSWrapper( is ) ;
        
        List<XLSRow> rows = xls.getRows( "click_stream",
                           new AND( new StrFilter( 1, "QUESTION_VISITED" ),
                                    new NumFilter( 0, NumFilter.LT, 156873 ))
                            ) ;
        XLSUtil.printRows( rows ) ;
        is.close() ;
    }
    
    public void execute() throws Exception {
        InputStream is = XLSWrapperTest.class.getResourceAsStream( resName ) ;
        XLSWrapper xls = new XLSWrapper( is ) ;
        
        List<String> qIds = xls.getColValues( "binding", null, "question_id" ) ;
        is.close() ;
        
        for( String id : qIds ) {
            log.debug( id ) ;
        }
    }
    
    public static void main( String[] args ) 
        throws Exception {
        new XLSWrapperTest().execute() ;
    }
}
