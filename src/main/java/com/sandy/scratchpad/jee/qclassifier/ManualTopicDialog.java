package com.sandy.scratchpad.jee.qclassifier;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

/**
 * Modal picker for a hand-chosen topic: every topic of the question's
 * syllabus, listed as {@code section › topic}, narrowed by a type-to-filter
 * field. The question's current top stored topic is preselected. Enter or a
 * double-click confirms; Esc cancels.
 */
@SuppressWarnings( "serial" )
final class ManualTopicDialog extends JDialog {

    private final List<Topic>              topics ;
    private final DefaultListModel<Topic>  listModel = new DefaultListModel<>() ;
    private final JList<Topic>             list      = new JList<>( listModel ) ;
    private final JTextField               filter    = new JTextField() ;
    private Topic chosen ;

    /** @return the chosen topic, or null if cancelled or the subject has no syllabus. */
    static Topic choose( Component parent, QTask task ) {
        String syllabus = TopicRepo.syllabusForSubject( task.getSubject() ) ;
        if( syllabus == null ) return null ;

        Integer currentId = null ;
        List<WorkspaceTopicMap.StoredMapping> stored =
                WorkspaceTopicMap.topicMappings( task.getSrcDir(), task.getQuestionId() ) ;
        if( !stored.isEmpty() ) currentId = stored.get( 0 ).topicId ;

        ManualTopicDialog d = new ManualTopicDialog( SwingUtilities.getWindowAncestor( parent ), task,
                TopicRepo.instance().getTopicsBySyllabus( syllabus ), currentId ) ;
        d.setVisible( true ) ;
        return d.chosen ;
    }

    private ManualTopicDialog( Window owner, QTask task, List<Topic> topics, Integer currentId ) {
        super( owner, "Manual Topic Association - " + task, ModalityType.APPLICATION_MODAL ) ;
        this.topics = topics ;

        list.setSelectionMode( ListSelectionModel.SINGLE_SELECTION ) ;
        list.setCellRenderer( new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent( JList<?> l, Object value, int index,
                                                           boolean sel, boolean focus ) {
                Topic t = (Topic) value ;
                return super.getListCellRendererComponent( l, t.getSection() + "  ›  " + t.getTopicName(),
                                                           index, sel, focus ) ;
            }
        } ) ;
        list.addMouseListener( new MouseAdapter() {
            @Override public void mouseClicked( MouseEvent e ) {
                if( e.getClickCount() == 2 ) confirm() ;
            }
        } ) ;

        filter.getDocument().addDocumentListener( new DocumentListener() {
            @Override public void insertUpdate( DocumentEvent e )  { applyFilter() ; }
            @Override public void removeUpdate( DocumentEvent e )  { applyFilter() ; }
            @Override public void changedUpdate( DocumentEvent e ) { applyFilter() ; }
        } ) ;
        // Up / Down in the filter field move the list selection.
        bindListMove( KeyEvent.VK_UP,   -1 ) ;
        bindListMove( KeyEvent.VK_DOWN, +1 ) ;

        JButton ok     = new JButton( "OK" ) ;
        JButton cancel = new JButton( "Cancel" ) ;
        ok.addActionListener( e -> confirm() ) ;
        cancel.addActionListener( e -> dispose() ) ;
        ok.setEnabled( false ) ;
        list.addListSelectionListener( e -> ok.setEnabled( list.getSelectedValue() != null ) ) ;
        getRootPane().setDefaultButton( ok ) ;
        getRootPane().registerKeyboardAction( e -> dispose(),
                KeyStroke.getKeyStroke( KeyEvent.VK_ESCAPE, 0 ), JComponent.WHEN_IN_FOCUSED_WINDOW ) ;

        JPanel top = new JPanel( new BorderLayout( 6, 0 ) ) ;
        top.add( new JLabel( "Filter:" ), BorderLayout.WEST ) ;
        top.add( filter, BorderLayout.CENTER ) ;

        JPanel buttons = new JPanel( new FlowLayout( FlowLayout.RIGHT ) ) ;
        buttons.add( cancel ) ;
        buttons.add( ok ) ;

        JPanel content = new JPanel( new BorderLayout( 0, 6 ) ) ;
        content.setBorder( BorderFactory.createEmptyBorder( 8, 8, 8, 8 ) ) ;
        content.add( top, BorderLayout.NORTH ) ;
        content.add( new JScrollPane( list ), BorderLayout.CENTER ) ;
        content.add( buttons, BorderLayout.SOUTH ) ;
        setContentPane( content ) ;

        applyFilter() ;
        if( currentId != null ) {
            for( int i = 0 ; i < listModel.size() ; i++ ) {
                if( listModel.get( i ).getId() == currentId ) {
                    list.setSelectedIndex( i ) ;
                    list.ensureIndexIsVisible( i ) ;
                    break ;
                }
            }
        }

        setSize( 560, 520 ) ;
        setLocationRelativeTo( owner ) ;
    }

    private void applyFilter() {
        Topic selected = list.getSelectedValue() ;
        String text = filter.getText().trim().toLowerCase() ;
        listModel.clear() ;
        for( Topic t : topics ) {
            if( text.isEmpty()
                    || t.getTopicName().toLowerCase().contains( text )
                    || ( t.getSection() != null && t.getSection().toLowerCase().contains( text ) ) ) {
                listModel.addElement( t ) ;
            }
        }
        int idx = selected != null ? listModel.indexOf( selected ) : -1 ;
        if( idx < 0 && !text.isEmpty() && !listModel.isEmpty() ) idx = 0 ;
        if( idx >= 0 ) {
            list.setSelectedIndex( idx ) ;
            list.ensureIndexIsVisible( idx ) ;
        }
    }

    private void bindListMove( int keyCode, int delta ) {
        String key = "move" + delta ;
        filter.getInputMap().put( KeyStroke.getKeyStroke( keyCode, 0 ), key ) ;
        filter.getActionMap().put( key, new AbstractAction() {
            @Override public void actionPerformed( java.awt.event.ActionEvent e ) {
                if( listModel.isEmpty() ) return ;
                int i = Math.max( 0, Math.min( listModel.size() - 1, list.getSelectedIndex() + delta ) ) ;
                list.setSelectedIndex( i ) ;
                list.ensureIndexIsVisible( i ) ;
            }
        } ) ;
    }

    private void confirm() {
        chosen = list.getSelectedValue() ;
        if( chosen != null ) dispose() ;
    }
}
