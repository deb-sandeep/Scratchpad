package com.sandy.scratchpad.jee.qclassifier;

import javax.swing.table.AbstractTableModel;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Table model backing {@link QClassifierFrame}. Every mutating method must be
 * called on the Swing event dispatch thread.
 */
@SuppressWarnings( "serial" )
public class QTaskTableModel extends AbstractTableModel {

    private static final String[] COLS =
            { "#", "Source", "Question", "Subj", "Status", "Attempts", "Time (s)", "Result" } ;

    static final class Row {
        final QTask task ;
        TaskStatus  status ;
        int         attempts ;
        long        startedAtMs ;
        long        finishedAtMs ;
        String      result = "" ;

        Row( QTask task, TaskStatus status ) {
            this.task = task ;
            this.status = status ;
        }

        double elapsedSeconds() {
            if( startedAtMs == 0 ) return -1 ;
            long end = finishedAtMs > 0 ? finishedAtMs : System.currentTimeMillis() ;
            return ( end - startedAtMs ) / 1000.0 ;
        }
    }

    private final List<Row>       rows    = new ArrayList<>() ;
    private final Map<QTask,Row>  byTask  = new HashMap<>() ;

    public void discovered( QTask task, boolean alreadyClassified ) {
        if( byTask.containsKey( task ) ) return ;
        Row row = new Row( task, alreadyClassified ? TaskStatus.SKIPPED : TaskStatus.QUEUED ) ;
        rows.add( row ) ;
        byTask.put( task, row ) ;
        int idx = rows.size() - 1 ;
        fireTableRowsInserted( idx, idx ) ;
    }

    public void started( QTask task ) {
        Row row = byTask.get( task ) ;
        if( row == null ) return ;
        row.status       = TaskStatus.RUNNING ;
        row.startedAtMs  = System.currentTimeMillis() ;
        row.finishedAtMs = 0 ;
        row.result       = "" ;
        fireRowUpdated( row ) ;
    }

    public void finished( QTask task, TaskStatus status, int attempts, String result ) {
        Row row = byTask.get( task ) ;
        if( row == null ) return ;
        row.status       = status ;
        row.attempts     = attempts ;
        row.finishedAtMs = System.currentTimeMillis() ;
        row.result       = result != null ? result : "" ;
        fireRowUpdated( row ) ;
    }

    /** Repaint the elapsed-time cell of rows still running. */
    public void tickRunningRows() {
        for( int i = 0 ; i < rows.size() ; i++ ) {
            if( rows.get( i ).status == TaskStatus.RUNNING ) {
                fireTableCellUpdated( i, 6 ) ;
            }
        }
    }

    public Row rowAt( int modelIndex ) {
        return rows.get( modelIndex ) ;
    }

    public Map<TaskStatus,Integer> statusCounts() {
        Map<TaskStatus,Integer> counts = new EnumMap<>( TaskStatus.class ) ;
        for( TaskStatus s : TaskStatus.values() ) counts.put( s, 0 ) ;
        for( Row r : rows ) counts.merge( r.status, 1, Integer::sum ) ;
        return counts ;
    }

    private void fireRowUpdated( Row row ) {
        int idx = rows.indexOf( row ) ;
        if( idx >= 0 ) fireTableRowsUpdated( idx, idx ) ;
    }

    // ---- AbstractTableModel ----------------------------------------------

    @Override public int      getRowCount()            { return rows.size() ; }
    @Override public int      getColumnCount()         { return COLS.length ; }
    @Override public String   getColumnName( int c )   { return COLS[c] ; }
    @Override public boolean  isCellEditable( int r, int c ) { return false ; }

    @Override
    public Class<?> getColumnClass( int c ) {
        return c == 0 ? Integer.class : ( c == 4 ? TaskStatus.class : String.class ) ;
    }

    @Override
    public Object getValueAt( int r, int c ) {
        Row row = rows.get( r ) ;
        switch( c ) {
            case 0:  return r + 1 ;
            case 1:  return row.task.getSourceName() ;
            case 2:  return row.task.getQuestionId() ;
            case 3:  return row.task.getSubject() ;
            case 4:  return row.status ;
            case 5:  return row.attempts > 0 ? String.valueOf( row.attempts ) : "" ;
            case 6:  {
                double s = row.elapsedSeconds() ;
                return s < 0 ? "" : String.format( "%.1f", s ) ;
            }
            case 7:  return row.result ;
            default: return "" ;
        }
    }
}
