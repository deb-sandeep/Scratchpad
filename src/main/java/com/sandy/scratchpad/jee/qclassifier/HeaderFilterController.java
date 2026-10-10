package com.sandy.scratchpad.jee.qclassifier;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.sandy.scratchpad.jee.qclassifier.QTaskTreeTableModel.COL_STATUS;
import static com.sandy.scratchpad.jee.qclassifier.QTaskTreeTableModel.COL_SUBJ;

/**
 * Sorting and per-column filtering driven from the table header of the
 * {@link QTaskTreeTableModel} tree table. Left-click a header to cycle its sort
 * (ascending, descending, discovery order); right-click for a filter popup.
 * Sorting and filtering apply to questions within each source; a source is
 * hidden while none of its questions pass the filters.
 * <p>
 * Text columns filter by "contains" (case-insensitive; the name column matches
 * the source name or question id); {@code Subj} and {@code Status} filter by a
 * set of allowed values. A filtered column's header is shown in bold with a
 * trailing {@code *}; the sorted column carries ▲ / ▼.
 */
class HeaderFilterController {

    private final JTable              table ;
    private final QTaskTreeTableModel model ;
    /** Invoked after the filter or sort changed, to rebuild the view. */
    private final Runnable            onChange ;

    /** model column -> "contains" text. */
    private final Map<Integer,String> textFilters = new LinkedHashMap<>() ;
    /** model column -> allowed values (absent = all allowed). */
    private final Map<Integer,Set<String>> valueFilters = new LinkedHashMap<>() ;
    /** model column -> the full set of possible values, for enumerable columns. */
    private final Map<Integer,List<String>> enumerable = new LinkedHashMap<>() ;

    HeaderFilterController( JTable table, QTaskTreeTableModel model, Runnable onChange ) {
        this.table    = table ;
        this.model    = model ;
        this.onChange = onChange ;

        enumerable.put( COL_SUBJ, List.of( "C", "M", "P", "?" ) ) ;
        enumerable.put( COL_STATUS, Arrays.stream( TaskStatus.values() )
                .map( Enum::name ).collect( Collectors.toList() ) ) ;

        JTableHeader header = table.getTableHeader() ;
        header.setReorderingAllowed( false ) ;
        header.setToolTipText( "Left-click to sort, right-click to filter" ) ;
        header.addMouseListener( new MouseAdapter() {
            @Override public void mousePressed( MouseEvent e )  { maybePopup( e ) ; }
            @Override public void mouseReleased( MouseEvent e ) { maybePopup( e ) ; }
            @Override public void mouseClicked( MouseEvent e ) {
                if( SwingUtilities.isLeftMouseButton( e ) && !e.isPopupTrigger() ) cycleSort( e ) ;
            }
        } ) ;

        TableCellRenderer base = header.getDefaultRenderer() ;
        header.setDefaultRenderer( ( t, value, sel, foc, row, viewCol ) -> {
            Component c = base.getTableCellRendererComponent( t, value, sel, foc, row, viewCol ) ;
            if( c instanceof JLabel ) {
                JLabel label = (JLabel) c ;
                int modelCol = t.convertColumnIndexToModel( viewCol ) ;
                boolean filtered = hasFilter( modelCol ) ;
                String text = String.valueOf( value ) ;
                if( model.getSortColumn() == modelCol ) text += model.isSortAscending() ? "  ▲" : "  ▼" ;
                if( filtered ) text += "  *" ;
                label.setText( text ) ;
                label.setFont( label.getFont().deriveFont( filtered ? Font.BOLD : Font.PLAIN ) ) ;
            }
            return c ;
        } ) ;
    }

    boolean hasAnyFilter() {
        return !textFilters.isEmpty() || !valueFilters.isEmpty() ;
    }

    void clearAll() {
        textFilters.clear() ;
        valueFilters.clear() ;
        apply() ;
    }

    // ---- sorting -----------------------------------------------------

    private void cycleSort( MouseEvent e ) {
        JTableHeader header = table.getTableHeader() ;
        if( header.getCursor().getType() != java.awt.Cursor.DEFAULT_CURSOR ) return ;   // resizing
        int viewCol = header.columnAtPoint( e.getPoint() ) ;
        if( viewCol < 0 ) return ;
        int modelCol = table.convertColumnIndexToModel( viewCol ) ;

        if( model.getSortColumn() != modelCol ) model.setSort( modelCol, true ) ;
        else if( model.isSortAscending() )      model.setSort( modelCol, false ) ;
        else                                    model.setSort( -1, true ) ;

        header.repaint() ;
        onChange.run() ;
    }

    // ---- popup -------------------------------------------------------

    private void maybePopup( MouseEvent e ) {
        if( !e.isPopupTrigger() ) return ;
        JTableHeader header = table.getTableHeader() ;
        int viewCol = header.columnAtPoint( e.getPoint() ) ;
        if( viewCol < 0 ) return ;
        int modelCol = table.convertColumnIndexToModel( viewCol ) ;

        JPopupMenu menu = enumerable.containsKey( modelCol )
                ? buildValueMenu( modelCol )
                : buildTextMenu( modelCol ) ;

        menu.addSeparator() ;
        JMenuItem clearAll = new JMenuItem( "Clear all filters" ) ;
        clearAll.setEnabled( hasAnyFilter() ) ;
        clearAll.addActionListener( a -> clearAll() ) ;
        menu.add( clearAll ) ;

        Rectangle r = header.getHeaderRect( viewCol ) ;
        menu.show( header, r.x, r.height ) ;
    }

    private JPopupMenu buildTextMenu( int modelCol ) {
        JPopupMenu menu = new JPopupMenu() ;
        String name = table.getModel().getColumnName( modelCol ) ;
        String current = textFilters.getOrDefault( modelCol, "" ) ;

        JMenuItem edit = new JMenuItem(
                current.isEmpty() ? "Filter “" + name + "”…"
                                  : "Filter “" + name + "” (“" + current + "”)…" ) ;
        edit.addActionListener( a -> {
            Object in = JOptionPane.showInputDialog( table, name + " contains:", "Filter " + name,
                    JOptionPane.PLAIN_MESSAGE, null, null, current ) ;
            if( in == null ) return ;
            String text = in.toString().trim() ;
            if( text.isEmpty() ) textFilters.remove( modelCol ) ;
            else                 textFilters.put( modelCol, text ) ;
            apply() ;
        } ) ;
        menu.add( edit ) ;

        if( !current.isEmpty() ) {
            JMenuItem clear = new JMenuItem( "Clear “" + name + "” filter" ) ;
            clear.addActionListener( a -> { textFilters.remove( modelCol ) ; apply() ; } ) ;
            menu.add( clear ) ;
        }
        return menu ;
    }

    private JPopupMenu buildValueMenu( int modelCol ) {
        JPopupMenu menu = new JPopupMenu() ;
        List<String> all = enumerable.get( modelCol ) ;
        Set<String> allowed = valueFilters.getOrDefault( modelCol, new LinkedHashSet<>( all ) ) ;

        JMenuItem showAll = new JMenuItem( "Show all" ) ;
        showAll.setEnabled( valueFilters.containsKey( modelCol ) ) ;
        showAll.addActionListener( a -> { valueFilters.remove( modelCol ) ; apply() ; } ) ;
        menu.add( showAll ) ;
        menu.addSeparator() ;

        for( String v : all ) {
            JMenuItem only = new JMenuItem( "Only " + v ) ;
            only.addActionListener( a -> {
                valueFilters.put( modelCol, new LinkedHashSet<>( List.of( v ) ) ) ;
                apply() ;
            } ) ;
            menu.add( only ) ;
        }
        menu.addSeparator() ;

        for( String v : all ) {
            JCheckBoxMenuItem cb = new JCheckBoxMenuItem( v, allowed.contains( v ) ) ;
            cb.addActionListener( a -> {
                Set<String> set = new LinkedHashSet<>( valueFilters.getOrDefault( modelCol, new LinkedHashSet<>( all ) ) ) ;
                if( cb.isSelected() ) set.add( v ) ;
                else                  set.remove( v ) ;
                if( set.size() == all.size() ) valueFilters.remove( modelCol ) ;
                else                           valueFilters.put( modelCol, set ) ;
                apply() ;
            } ) ;
            menu.add( cb ) ;
        }
        return menu ;
    }

    // ---- filter application ----------------------------------------

    private boolean hasFilter( int modelCol ) {
        return textFilters.containsKey( modelCol ) || valueFilters.containsKey( modelCol ) ;
    }

    private void apply() {
        Predicate<QTaskTreeTableModel.QuestionNode> filter = null ;
        if( hasAnyFilter() ) {
            Map<Integer,String> texts = new LinkedHashMap<>() ;
            textFilters.forEach( ( col, text ) -> texts.put( col, text.toLowerCase() ) ) ;
            Map<Integer,Set<String>> values = new LinkedHashMap<>( valueFilters ) ;

            filter = q -> {
                for( Map.Entry<Integer,String> e : texts.entrySet() ) {
                    if( !model.filterText( q, e.getKey() ).toLowerCase().contains( e.getValue() ) ) return false ;
                }
                for( Map.Entry<Integer,Set<String>> e : values.entrySet() ) {
                    if( !e.getValue().contains( String.valueOf( model.valueAt( q, e.getKey() ) ) ) ) return false ;
                }
                return true ;
            } ;
        }
        model.setFilter( filter ) ;
        table.getTableHeader().repaint() ;
        onChange.run() ;
    }
}
