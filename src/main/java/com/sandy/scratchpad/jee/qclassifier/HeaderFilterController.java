package com.sandy.scratchpad.jee.qclassifier;

import javax.swing.*;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.Component;
import java.awt.Font;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Per-column row filtering driven from the table header. Left-click a header to
 * sort (default behaviour); right-click a header for a filter popup. Text columns
 * filter by "contains" (case-insensitive); {@code Subj} and {@code Status} filter
 * by a set of allowed values. A filtered column's header is shown in bold with a
 * trailing marker.
 */
class HeaderFilterController {

    private final JTable table ;
    private final TableRowSorter<QTaskTableModel> sorter ;

    /** model column -> "contains" text. */
    private final Map<Integer,String> textFilters = new LinkedHashMap<>() ;
    /** model column -> allowed values (absent = all allowed). */
    private final Map<Integer,Set<String>> valueFilters = new LinkedHashMap<>() ;
    /** model column -> the full set of possible values, for enumerable columns. */
    private final Map<Integer,List<String>> enumerable = new LinkedHashMap<>() ;

    @SuppressWarnings( "unchecked" )
    HeaderFilterController( JTable table ) {
        this.table  = table ;
        this.sorter = (TableRowSorter<QTaskTableModel>) table.getRowSorter() ;

        enumerable.put( 3, List.of( "C", "M", "P", "?" ) ) ;
        enumerable.put( 4, Arrays.stream( TaskStatus.values() )
                .map( Enum::name ).collect( Collectors.toList() ) ) ;

        JTableHeader header = table.getTableHeader() ;
        header.setToolTipText( "Left-click to sort, right-click to filter" ) ;
        header.addMouseListener( new MouseAdapter() {
            @Override public void mousePressed( MouseEvent e )  { maybePopup( e ) ; }
            @Override public void mouseReleased( MouseEvent e ) { maybePopup( e ) ; }
        } ) ;

        TableCellRenderer base = header.getDefaultRenderer() ;
        header.setDefaultRenderer( ( t, value, sel, foc, row, viewCol ) -> {
            Component c = base.getTableCellRendererComponent( t, value, sel, foc, row, viewCol ) ;
            if( c instanceof JLabel ) {
                JLabel label = (JLabel) c ;
                int modelCol = t.convertColumnIndexToModel( viewCol ) ;
                boolean filtered = hasFilter( modelCol ) ;
                label.setText( filtered ? value + "  ▼" : String.valueOf( value ) ) ;
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
        List<RowFilter<Object,Object>> parts = new ArrayList<>() ;

        textFilters.forEach( ( col, text ) ->
                parts.add( RowFilter.regexFilter( "(?i)" + Pattern.quote( text ), col ) ) ) ;

        valueFilters.forEach( ( col, allowed ) -> parts.add( new RowFilter<>() {
            @Override public boolean include( Entry<? extends Object, ? extends Object> entry ) {
                return allowed.contains( String.valueOf( entry.getValue( col ) ) ) ;
            }
        } ) ) ;

        sorter.setRowFilter( parts.isEmpty() ? null : RowFilter.andFilter( parts ) ) ;
        table.getTableHeader().repaint() ;
    }
}
