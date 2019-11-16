package com.sandy.scratchpad.sconsole.lapreconstruct;

import static com.sandy.common.xlsutil.filter.NumFilter.* ;

import java.io.InputStream ;
import java.util.HashMap ;
import java.util.LinkedHashMap ;
import java.util.List ;

import org.apache.commons.lang.StringUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.xlsutil.XLSRow ;
import com.sandy.common.xlsutil.XLSRowFilter ;
import com.sandy.common.xlsutil.XLSUtil ;
import com.sandy.common.xlsutil.XLSWrapper ;
import com.sandy.common.xlsutil.XLSRowFilter.AND ;
import com.sandy.common.xlsutil.filter.NumFilter ;
import com.sandy.common.xlsutil.filter.StrFilter ;

class LapState {
    public int timeSpent = 0 ;
    public String questionState = "q-not-visited" ;
}

public class LapReconstruct {
    
    private static final Logger log = Logger.getLogger( LapReconstruct.class ) ;
    
    private static HashMap<String, String> STATUS_LOOKUP = new HashMap<>() ;
    static {
        STATUS_LOOKUP.put( "ANSWER_SAVE", "q-attempted" ) ;
        STATUS_LOOKUP.put( "ANSWER_MARK_FOR_REVIEW", "q-marked-for-review" ) ;
        STATUS_LOOKUP.put( "ANSWER_CLEAR_RESPONSE", "q-not-answered" ) ;
        STATUS_LOOKUP.put( "ANSWER_SAVE_AND_MARK_REVIEW", "q-ans-and-marked-for-review" ) ;
        STATUS_LOOKUP.put( "", "q-not-visited" ) ;
    }
    
    private LinkedHashMap<String, LapState[]> lapStates = new LinkedHashMap<>() ;

    public void execute( XLSWrapper xls ) throws Exception {
        prePopulateLapStates( xls ) ;
        populateLap1Details( xls ) ;
        populateLap2Details( xls ) ;
        
        StringBuilder buffer = new StringBuilder() ;
        int qNo = 1 ;
        for( String key : lapStates.keySet() ) {
            
            LapState l0 = lapStates.get( key )[0] ;
            LapState l1 = lapStates.get( key )[1] ;
            
            buffer.append( StringUtils.rightPad( "Q" + qNo++, 5 ) )
                  .append( StringUtils.rightPad( key, 12 ) ) 
                  .append( " - " )
                  .append( StringUtils.leftPad( String.valueOf( l0.timeSpent ), 10 ) )
                  .append( "  " )
                  .append( StringUtils.rightPad( l0.questionState, 30 ) )
                  .append( " - " )
                  .append( StringUtils.leftPad( String.valueOf( l1.timeSpent ), 10 ) )
                  .append( "  " )
                  .append( StringUtils.rightPad( l1.questionState, 30 ) )
                  .append( "\n" ) ;
        }
        log.debug( buffer ) ;
        
        generateReconstructionQueries() ;
    } 
    
    private void generateReconstructionQueries() {
        
        for( String key : lapStates.keySet() ) {
            
            LapState l0 = lapStates.get( key )[0] ;
            LapState l1 = lapStates.get( key )[1] ;
            
            printSQL( key, l0, "L1" ) ;
            printSQL( key, l1, "L2" ) ;
        }
    }
    
    private void printSQL( String qId, LapState state, String lapName ) {
        
        String sql = "UPDATE test_attempt_lap_snapshot "
                   + "SET "
                   + "time_spent = " + (int)(state.timeSpent/1000) + ", "
                   + "attempt_status = '" + state.questionState + "' "
                   + "WHERE "
                   + "test_attempt_id = 119994 AND "
                   + "question_id = "+ qId +" AND "
                   + "lap_name = '"+ lapName +"' ; " ;
        log.debug( sql ) ;
    }
    
    private void prePopulateLapStates( XLSWrapper xls ) {
        List<String> qIds = xls.getColValues( "binding", null, "question_id" ) ;
        for( String id : qIds ) {
            LapState[] states = { new LapState(), new LapState() } ;
            lapStates.put( id, states ) ;
        }
    }
    
    private void populateLap1Details( XLSWrapper xls ) {

        XLSRowFilter f1 = new NumFilter( 0, LTE, 1740000 ) ;

        XLSRowFilter filter = new AND( new StrFilter( 1, "QUESTION_VISITED" ), f1 ) ;
        List<XLSRow> rows = xls.getRows( "click_stream", filter ) ;
        addLapDetails( 0, rows, 1740000 ) ;
        
        filter = new AND( new StrFilter( 1, "ANSWER_.*" ), f1 ) ;
        rows = xls.getRows( "click_stream", filter ) ;
        addLapState( 0, rows ) ;
    }
    
    private void populateLap2Details( XLSWrapper xls ) {
        
        XLSRowFilter f1 = new AND( new NumFilter( 0, GTE, 1740000 ),
                                   new NumFilter( 0, LTE, 8994813 ) ) ;
        
        XLSRowFilter filter = new AND( new StrFilter( 1, "QUESTION_VISITED" ), f1 ) ;
        List<XLSRow> rows = xls.getRows( "click_stream", filter ) ;
        addLapDetails( 1, rows, 8994813 ) ;

        filter = new AND( new StrFilter( 1, "ANSWER_.*" ), f1 ) ;
        rows = xls.getRows( "click_stream", filter ) ;
        addLapState( 1, rows ) ;
    }
    
    
    private void addLapDetails( int lapIndex, List<XLSRow> rows, int lapEndMarker ) {
        
        String lastVistedQId = null ;
        int lastTimeMarker = 0 ;
        
        for( XLSRow row : rows ) {
            String qId = row.getCellValue( "payload" ) ;
            int timeMarker = Integer.parseInt( row.getCellValue( "time_marker" ) ) ;
            
            if( lastVistedQId != null ) {
                int timeSpent = timeMarker - lastTimeMarker ;
                LapState state = lapStates.get( lastVistedQId )[lapIndex] ;
                state.timeSpent += timeSpent ;
            }
            
            lastVistedQId = qId ;
            lastTimeMarker = timeMarker ;
        }
        
        int timeSpent = lapEndMarker - lastTimeMarker ;
        LapState state = lapStates.get( lastVistedQId )[lapIndex] ;
        state.timeSpent += timeSpent ;
    }
    
    private void addLapState( int lapIndex, List<XLSRow> rows ) {
        
        for( XLSRow row : rows ) {
            String qId = row.getCellValue( "payload" ) ;
            String eventId = row.getCellValue( "event_id" ) ;
            
            LapState state = lapStates.get( qId )[lapIndex] ;
            state.questionState = STATUS_LOOKUP.get( eventId.trim() ) ;
            
            if( lapIndex == 0 ) {
                state = lapStates.get( qId )[1] ;
                state.questionState = STATUS_LOOKUP.get( eventId.trim() ) ;
            }
        }
    }
    
    public static void main( String[] args ) throws Exception {
        InputStream is = XLSWrapperTest.class.getResourceAsStream( "/LapRepair.xlsx" ) ;
        XLSWrapper xls = new XLSWrapper( is ) ;
        new LapReconstruct().execute( xls ) ;
        is.close() ;
    }
}
