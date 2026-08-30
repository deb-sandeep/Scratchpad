package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Map;

/**
 * Live monitor for {@link QImgClassifier}. One row per question image; rows move
 * QUEUED -> RUNNING -> DONE/FAILED (or start SKIPPED if already classified).
 * Double-click opens the image; right-click offers open / re-queue.
 */
@Slf4j
@SuppressWarnings( "serial" )
public class QClassifierFrame extends JFrame implements ClassificationListener {

    /** Wiring back to the engine so the UI can re-queue tasks and shut down. */
    public interface Controller {
        void reprocess( QTask task ) ;
        void shutdown() ;
    }

    private final QTaskTableModel model = new QTaskTableModel() ;
    private final JTable          table ;
    private final JLabel          statusBar = new JLabel( " " ) ;
    private final long            startMs = System.currentTimeMillis() ;

    private Controller controller ;
    private volatile String batchProgress = "" ;

    public QClassifierFrame() {
        super( "Gemma Question Classifier" ) ;

        table = new JTable( model ) {
            @Override
            public Component prepareRenderer( javax.swing.table.TableCellRenderer r, int row, int col ) {
                Component c = super.prepareRenderer( r, row, col ) ;
                if( !isRowSelected( row ) ) {
                    QTaskTableModel.Row mr = model.rowAt( convertRowIndexToModel( row ) ) ;
                    c.setBackground( colorFor( mr.status ) ) ;
                }
                return c ;
            }
        } ;
        table.setAutoCreateRowSorter( false ) ;
        table.setRowSorter( new TableRowSorter<>( model ) ) ;
        table.setFillsViewportHeight( true ) ;

        DefaultTableCellRenderer center = new DefaultTableCellRenderer() ;
        center.setHorizontalAlignment( SwingConstants.CENTER ) ;
        for( int c : new int[]{ 0, 3, 4, 5, 6 } ) {
            table.getColumnModel().getColumn( c ).setCellRenderer( center ) ;
        }
        int[] widths = { 40, 150, 130, 45, 80, 70, 70, 380 } ;
        for( int i = 0 ; i < widths.length ; i++ ) {
            table.getColumnModel().getColumn( i ).setPreferredWidth( widths[i] ) ;
        }

        installMouseHandlers() ;

        setLayout( new BorderLayout() ) ;
        add( new JScrollPane( table ), BorderLayout.CENTER ) ;
        statusBar.setBorder( BorderFactory.createEmptyBorder( 4, 8, 4, 8 ) ) ;
        add( statusBar, BorderLayout.SOUTH ) ;

        setBounds( 100, 100, 1100, 700 ) ;
        setDefaultCloseOperation( WindowConstants.DO_NOTHING_ON_CLOSE ) ;
        addWindowListener( new WindowAdapter() {
            @Override public void windowClosing( WindowEvent e ) {
                if( controller != null ) controller.shutdown() ;
                dispose() ;
                System.exit( 0 ) ;
            }
        } ) ;

        new Timer( 1000, e -> { model.tickRunningRows() ; refreshStatusBar() ; } ).start() ;
        refreshStatusBar() ;
    }

    public void setController( Controller controller ) {
        this.controller = controller ;
    }

    // ---- ClassificationListener (marshalled onto the EDT) ---------------

    @Override
    public void taskDiscovered( QTask task, boolean alreadyClassified ) {
        SwingUtilities.invokeLater( () -> { model.discovered( task, alreadyClassified ) ; refreshStatusBar() ; } ) ;
    }

    @Override
    public void taskStarted( QTask task ) {
        SwingUtilities.invokeLater( () -> { model.started( task ) ; refreshStatusBar() ; } ) ;
    }

    @Override
    public void taskFinished( QTask task, TaskStatus status, int attempts, String result ) {
        SwingUtilities.invokeLater( () -> { model.finished( task, status, attempts, result ) ; refreshStatusBar() ; } ) ;
    }

    @Override
    public void batchProgress( int sourceIndex, int sourceCount, String sourceName ) {
        batchProgress = String.format( "source %d/%d - %s", sourceIndex, sourceCount, sourceName ) ;
        SwingUtilities.invokeLater( this::refreshStatusBar ) ;
    }

    // ---- UI plumbing ---------------------------------------------------

    private void installMouseHandlers() {
        table.addMouseListener( new MouseAdapter() {
            @Override public void mouseClicked( MouseEvent e ) {
                if( e.getClickCount() == 2 && SwingUtilities.isLeftMouseButton( e ) ) {
                    QTaskTableModel.Row row = rowUnder( e ) ;
                    if( row != null ) openImage( row.task ) ;
                }
            }
            @Override public void mousePressed( MouseEvent e )  { maybePopup( e ) ; }
            @Override public void mouseReleased( MouseEvent e ) { maybePopup( e ) ; }
        } ) ;
    }

    private void maybePopup( MouseEvent e ) {
        if( !e.isPopupTrigger() ) return ;
        int viewRow = table.rowAtPoint( e.getPoint() ) ;
        if( viewRow < 0 ) return ;
        table.setRowSelectionInterval( viewRow, viewRow ) ;
        QTaskTableModel.Row row = model.rowAt( table.convertRowIndexToModel( viewRow ) ) ;

        JPopupMenu menu = new JPopupMenu() ;
        JMenuItem open = new JMenuItem( "Open image" ) ;
        open.addActionListener( a -> openImage( row.task ) ) ;
        menu.add( open ) ;

        JMenuItem requeue = new JMenuItem( "Re-queue" ) ;
        boolean canRequeue = controller != null &&
                ( row.status == TaskStatus.DONE || row.status == TaskStatus.FAILED
                        || row.status == TaskStatus.SKIPPED ) ;
        requeue.setEnabled( canRequeue ) ;
        requeue.addActionListener( a -> controller.reprocess( row.task ) ) ;
        menu.add( requeue ) ;

        menu.show( table, e.getX(), e.getY() ) ;
    }

    private QTaskTableModel.Row rowUnder( MouseEvent e ) {
        int viewRow = table.rowAtPoint( e.getPoint() ) ;
        return viewRow < 0 ? null : model.rowAt( table.convertRowIndexToModel( viewRow ) ) ;
    }

    private void openImage( QTask task ) {
        try {
            Desktop.getDesktop().open( task.getImgFile() ) ;
        }
        catch( Exception ex ) {
            log.error( "Could not open image {}", task.getImgFile(), ex ) ;
            JOptionPane.showMessageDialog( this, "Could not open image:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE ) ;
        }
    }

    private void refreshStatusBar() {
        Map<TaskStatus,Integer> c = model.statusCounts() ;
        long elapsed = ( System.currentTimeMillis() - startMs ) / 1000 ;
        String text = String.format(
                "Total %d   |   Queued %d   Running %d   Done %d   Failed %d   Skipped %d   |   Elapsed %d:%02d",
                model.getRowCount(),
                c.get( TaskStatus.QUEUED ), c.get( TaskStatus.RUNNING ), c.get( TaskStatus.DONE ),
                c.get( TaskStatus.FAILED ), c.get( TaskStatus.SKIPPED ),
                elapsed / 60, elapsed % 60 ) ;
        if( !batchProgress.isEmpty() ) text += "   |   " + batchProgress ;
        statusBar.setText( text ) ;
    }

    private static Color colorFor( TaskStatus status ) {
        switch( status ) {
            case RUNNING: return new Color( 0xE3F2FD ) ;
            case DONE:    return new Color( 0xE8F5E9 ) ;
            case FAILED:  return new Color( 0xFFEBEE ) ;
            case SKIPPED: return new Color( 0xF0F0F0 ) ;
            default:      return Color.WHITE ;
        }
    }
}
