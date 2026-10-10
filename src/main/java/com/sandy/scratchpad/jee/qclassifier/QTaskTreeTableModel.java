package com.sandy.scratchpad.jee.qclassifier;

import javax.swing.table.AbstractTableModel;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Tree-shaped table model backing {@link QClassifierFrame}: sources at the top
 * level, their questions beneath, and - for multi-part questions - the part
 * images beneath the question (collapsed by default). Sources start collapsed;
 * a source expands when one of its questions starts running and collapses again
 * once none of its questions is QUEUED or RUNNING. The tree is flattened
 * into the list of currently visible rows; filtering and sorting apply to
 * questions within each source.
 * <p>
 * Every method must be called on the Swing event dispatch thread. Structural
 * changes ({@link #discovered}, {@link #setFilter}, {@link #setSort},
 * {@link #setAllSourcesExpanded}) only take effect on the next {@link #rebuild()}.
 */
@SuppressWarnings( "serial" )
public class QTaskTreeTableModel extends AbstractTableModel {

    static final int COL_NAME     = 0 ;
    static final int COL_SUBJ     = 1 ;
    static final int COL_STATUS   = 2 ;
    static final int COL_ATTEMPTS = 3 ;
    static final int COL_TIME     = 4 ;
    static final int COL_RESULT   = 5 ;

    private static final String[] COLS =
            { "Source / Question", "Subj", "Status", "Attempts", "Time (s)", "Result" } ;

    // ---- nodes -------------------------------------------------------

    abstract static class Node {
        /** Row index in the flattened view, -1 when not visible. */
        int     viewIndex = -1 ;
        boolean expanded ;

        abstract int    depth() ;
        abstract String label() ;
        boolean isExpandable() { return false ; }
    }

    static final class SourceNode extends Node {
        final String name ;
        final List<QuestionNode> children = new ArrayList<>() ;

        SourceNode( String name ) {
            this.name = name ;
            this.expanded = false ;   // expanded only while it is being processed
        }

        @Override int     depth()        { return 0 ; }
        @Override String  label()        { return name ; }
        @Override boolean isExpandable() { return true ; }

        /** True once no question is waiting or running (DONE / FAILED / ASSOCIATED only). */
        boolean isFullyProcessed() {
            for( QuestionNode q : children ) {
                if( q.status == TaskStatus.QUEUED || q.status == TaskStatus.RUNNING ) return false ;
            }
            return true ;
        }

        Map<TaskStatus,Integer> counts() {
            Map<TaskStatus,Integer> counts = new EnumMap<>( TaskStatus.class ) ;
            for( TaskStatus s : TaskStatus.values() ) counts.put( s, 0 ) ;
            for( QuestionNode q : children ) counts.merge( q.status, 1, Integer::sum ) ;
            return counts ;
        }

        /** Most "active" status among the children: RUNNING > QUEUED > FAILED > DONE > ASSOCIATED. */
        TaskStatus aggregateStatus() {
            Map<TaskStatus,Integer> c = counts() ;
            for( TaskStatus s : new TaskStatus[]{ TaskStatus.RUNNING, TaskStatus.QUEUED,
                                                  TaskStatus.FAILED, TaskStatus.DONE } ) {
                if( c.get( s ) > 0 ) return s ;
            }
            return TaskStatus.ASSOCIATED ;
        }

        String summary() {
            Map<TaskStatus,Integer> c = counts() ;
            StringBuilder sb = new StringBuilder().append( children.size() ).append( " questions" ) ;
            c.forEach( ( s, n ) -> { if( n > 0 ) sb.append( "  ·  " ).append( s ).append( ' ' ).append( n ) ; } ) ;
            return sb.toString() ;
        }
    }

    static final class QuestionNode extends Node {
        final SourceNode     parent ;
        final QTask          task ;
        final List<PartNode> parts = new ArrayList<>() ;
        TaskStatus status ;
        int        attempts ;
        long       startedAtMs ;
        long       finishedAtMs ;
        String     result = "" ;

        QuestionNode( SourceNode parent, QTask task, TaskStatus status ) {
            this.parent = parent ;
            this.task   = task ;
            this.status = status ;
            if( task.isMultiPart() ) {
                for( File f : task.getImgFiles() ) parts.add( new PartNode( this, f ) ) ;
            }
        }

        @Override int     depth()        { return 1 ; }
        @Override boolean isExpandable() { return !parts.isEmpty() ; }
        @Override String  label() {
            return parts.isEmpty() ? task.getQuestionId()
                                   : task.getQuestionId() + "   [" + parts.size() + " parts]" ;
        }

        double elapsedSeconds() {
            if( startedAtMs == 0 ) return -1 ;
            long end = finishedAtMs > 0 ? finishedAtMs : System.currentTimeMillis() ;
            return ( end - startedAtMs ) / 1000.0 ;
        }
    }

    static final class PartNode extends Node {
        final QuestionNode parent ;
        final File         file ;

        PartNode( QuestionNode parent, File file ) {
            this.parent = parent ;
            this.file   = file ;
        }

        @Override int    depth() { return 2 ; }
        @Override String label() { return FileHelper.extractQuestionId( file ) ; }
    }

    // ---- state -------------------------------------------------------

    private final List<SourceNode>        sources      = new ArrayList<>() ;
    private final Map<String,SourceNode>  sourceByDir  = new HashMap<>() ;
    private final Map<QTask,QuestionNode> byTask       = new HashMap<>() ;
    private final List<Node>              visible      = new ArrayList<>() ;
    private int                           matchingQuestions ;

    private Predicate<QuestionNode> filter ;     // null = everything passes
    private int     sortCol = -1 ;               // -1 = discovery order
    private boolean sortAsc = true ;

    // ---- task lifecycle --------------------------------------------

    /** {@code associatedResult} is null for a question not yet in the topic map. */
    public void discovered( QTask task, String associatedResult ) {
        if( byTask.containsKey( task ) ) return ;
        SourceNode src = sourceByDir.computeIfAbsent( task.getSrcDir().getAbsolutePath(), k -> {
            SourceNode s = new SourceNode( task.getSourceName() ) ;
            sources.add( s ) ;
            return s ;
        } ) ;
        QuestionNode q = new QuestionNode( src, task,
                associatedResult != null ? TaskStatus.ASSOCIATED : TaskStatus.QUEUED ) ;
        if( associatedResult != null ) q.result = associatedResult ;
        src.children.add( q ) ;
        byTask.put( task, q ) ;
    }

    public void started( QTask task ) {
        QuestionNode q = byTask.get( task ) ;
        if( q == null ) return ;
        q.status       = TaskStatus.RUNNING ;
        q.startedAtMs  = System.currentTimeMillis() ;
        q.finishedAtMs = 0 ;
        q.result       = "" ;
        setExpanded( q.parent, true ) ;     // the source being processed is shown open
        fireQuestionUpdated( q ) ;
    }

    public void finished( QTask task, TaskStatus status, int attempts, String result ) {
        QuestionNode q = byTask.get( task ) ;
        if( q == null ) return ;
        q.status       = status ;
        q.attempts     = attempts ;
        q.finishedAtMs = System.currentTimeMillis() ;
        q.result       = result != null ? result : "" ;
        if( q.parent.isFullyProcessed() ) setExpanded( q.parent, false ) ;
        fireQuestionUpdated( q ) ;
    }

    /** Repaint the elapsed-time cell of visible questions still running. */
    public void tickRunningRows() {
        for( QuestionNode q : byTask.values() ) {
            if( q.status == TaskStatus.RUNNING && q.viewIndex >= 0 ) {
                fireTableCellUpdated( q.viewIndex, COL_TIME ) ;
            }
        }
    }

    private void fireQuestionUpdated( QuestionNode q ) {
        if( q.viewIndex >= 0 )        fireTableRowsUpdated( q.viewIndex, q.viewIndex ) ;
        if( q.parent.viewIndex >= 0 ) fireTableRowsUpdated( q.parent.viewIndex, q.parent.viewIndex ) ;
        for( PartNode p : q.parts ) {
            if( p.viewIndex >= 0 ) fireTableRowsUpdated( p.viewIndex, p.viewIndex ) ;
        }
    }

    // ---- filter / sort / expansion ---------------------------------

    void setFilter( Predicate<QuestionNode> filter ) { this.filter = filter ; }

    void setSort( int col, boolean ascending ) {
        this.sortCol = col ;
        this.sortAsc = ascending ;
    }

    int     getSortColumn()    { return sortCol ; }
    boolean isSortAscending()  { return sortAsc ; }

    /** True if a status change can move a row (so the view needs a {@link #rebuild()}). */
    boolean isFilteredOrSorted() { return filter != null || sortCol >= 0 ; }

    void setAllSourcesExpanded( boolean expanded ) {
        for( SourceNode s : sources ) s.expanded = expanded ;
    }

    /**
     * Expand / collapse one visible node in place, firing row insert / delete
     * events so the table keeps its selection.
     */
    void setExpanded( Node n, boolean expanded ) {
        if( !n.isExpandable() || n.expanded == expanded ) return ;
        n.expanded = expanded ;
        int idx = n.viewIndex ;
        if( idx < 0 ) return ;

        if( expanded ) {
            List<Node> sub = new ArrayList<>() ;
            appendChildren( n, sub ) ;
            visible.addAll( idx + 1, sub ) ;
            reindexFrom( idx + 1 ) ;
            if( !sub.isEmpty() ) fireTableRowsInserted( idx + 1, idx + sub.size() ) ;
        }
        else {
            int end = idx + 1 ;
            while( end < visible.size() && visible.get( end ).depth() > n.depth() ) end++ ;
            if( end > idx + 1 ) {
                List<Node> removed = visible.subList( idx + 1, end ) ;
                removed.forEach( r -> r.viewIndex = -1 ) ;
                removed.clear() ;
                reindexFrom( idx + 1 ) ;
                fireTableRowsDeleted( idx + 1, end - 1 ) ;
            }
        }
        fireTableRowsUpdated( idx, idx ) ;   // expander glyph
    }

    /** Recompute the flattened view from the tree, applying filter, sort and expansion. */
    void rebuild() {
        visible.forEach( n -> n.viewIndex = -1 ) ;
        visible.clear() ;
        matchingQuestions = 0 ;

        List<SourceNode> srcs = new ArrayList<>( sources ) ;
        if( sortCol == COL_NAME || sortCol == COL_STATUS ) srcs.sort( comparator() ) ;

        for( SourceNode s : srcs ) {
            List<QuestionNode> qs = matchingChildren( s ) ;
            if( filter != null && qs.isEmpty() ) continue ;
            matchingQuestions += qs.size() ;
            visible.add( s ) ;
            if( s.expanded ) appendQuestions( qs, visible ) ;
        }
        reindexFrom( 0 ) ;
        fireTableDataChanged() ;
    }

    private void appendChildren( Node n, List<Node> out ) {
        if( n instanceof SourceNode ) appendQuestions( matchingChildren( (SourceNode) n ), out ) ;
        else if( n instanceof QuestionNode ) out.addAll( ( (QuestionNode) n ).parts ) ;
    }

    private void appendQuestions( List<QuestionNode> qs, List<Node> out ) {
        for( QuestionNode q : qs ) {
            out.add( q ) ;
            if( q.expanded ) out.addAll( q.parts ) ;
        }
    }

    private List<QuestionNode> matchingChildren( SourceNode s ) {
        List<QuestionNode> qs = new ArrayList<>() ;
        for( QuestionNode q : s.children ) {
            if( filter == null || filter.test( q ) ) qs.add( q ) ;
        }
        if( sortCol >= 0 ) qs.sort( comparator() ) ;
        return qs ;
    }

    private void reindexFrom( int from ) {
        for( int i = from ; i < visible.size() ; i++ ) visible.get( i ).viewIndex = i ;
    }

    private <T extends Node> Comparator<T> comparator() {
        Comparator<T> c ;
        switch( sortCol ) {
            case COL_NAME:
                // Sources by name; questions by their numeric trailing sequence.
                c = ( a, b ) -> a instanceof QuestionNode
                        ? FileHelper.QUESTION_ORDER.compare( sortName( a ), sortName( b ) )
                        : FileHelper.naturalCompare( sortName( a ), sortName( b ) ) ;
                break ;
            case COL_STATUS:
                c = Comparator.comparing( n -> (TaskStatus) valueAt( n, COL_STATUS ) ) ;
                break ;
            case COL_ATTEMPTS:
                c = Comparator.comparingInt( n -> ( (QuestionNode) n ).attempts ) ;
                break ;
            case COL_TIME:
                c = Comparator.comparingDouble( n -> ( (QuestionNode) n ).elapsedSeconds() ) ;
                break ;
            default:
                c = Comparator.comparing( n -> String.valueOf( valueAt( n, sortCol ) ),
                                          String.CASE_INSENSITIVE_ORDER ) ;
        }
        return sortAsc ? c : c.reversed() ;
    }

    private static String sortName( Node n ) {
        return n instanceof QuestionNode ? ( (QuestionNode) n ).task.getQuestionId() : n.label() ;
    }

    // ---- queries ---------------------------------------------------

    Node nodeAt( int viewRow ) { return visible.get( viewRow ) ; }

    int getQuestionCount()         { return byTask.size() ; }
    int getMatchingQuestionCount() { return matchingQuestions ; }
    int getSourceCount()           { return sources.size() ; }

    public Map<TaskStatus,Integer> statusCounts() {
        Map<TaskStatus,Integer> counts = new EnumMap<>( TaskStatus.class ) ;
        for( TaskStatus s : TaskStatus.values() ) counts.put( s, 0 ) ;
        for( QuestionNode q : byTask.values() ) counts.merge( q.status, 1, Integer::sum ) ;
        return counts ;
    }

    /** Text a "contains" filter matches against; the name column matches source or question id. */
    String filterText( QuestionNode q, int col ) {
        if( col == COL_NAME ) return q.parent.name + "/" + q.task.getQuestionId() ;
        return String.valueOf( valueAt( q, col ) ) ;
    }

    Object valueAt( Node n, int c ) {
        if( n instanceof SourceNode ) {
            SourceNode s = (SourceNode) n ;
            switch( c ) {
                case COL_NAME:   return s.name ;
                case COL_STATUS: return s.aggregateStatus() ;
                case COL_RESULT: return s.summary() ;
                default:         return "" ;
            }
        }
        if( n instanceof PartNode ) {
            return c == COL_NAME ? n.label() : "" ;
        }
        QuestionNode q = (QuestionNode) n ;
        switch( c ) {
            case COL_NAME:     return q.task.getQuestionId() ;
            case COL_SUBJ:     return q.task.getSubject() ;
            case COL_STATUS:   return q.status ;
            case COL_ATTEMPTS: return q.attempts > 0 ? String.valueOf( q.attempts ) : "" ;
            case COL_TIME: {
                double s = q.elapsedSeconds() ;
                return s < 0 ? "" : String.format( "%.1f", s ) ;
            }
            case COL_RESULT:   return q.result ;
            default:           return "" ;
        }
    }

    // ---- AbstractTableModel ----------------------------------------

    @Override public int     getRowCount()                  { return visible.size() ; }
    @Override public int     getColumnCount()               { return COLS.length ; }
    @Override public String  getColumnName( int c )         { return COLS[c] ; }
    @Override public boolean isCellEditable( int r, int c ) { return false ; }

    @Override
    public Object getValueAt( int r, int c ) {
        return valueAt( visible.get( r ), c ) ;
    }
}
