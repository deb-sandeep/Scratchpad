package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Map;
import java.util.Objects;

/**
 * Live monitor for {@link QImgClassifier}. One row per question image; rows move
 * QUEUED -> RUNNING -> DONE/FAILED (or start SKIPPED if already classified).
 * Double-click opens the image; right-click offers open / re-queue.
 */
@Slf4j
@SuppressWarnings( "serial" )
public class QClassifierFrame extends JFrame implements ClassificationListener {

    /** Wiring back to the engine so the UI can drive and re-queue processing. */
    public interface Controller {
        void startProcessing() ;
        void stopProcessing() ;
        void reprocess( QTask task ) ;
        void shutdown() ;
    }

    private static final int PREVIEW_PANEL_HEIGHT = 300 ;

    private final QTaskTableModel model = new QTaskTableModel() ;
    private final JTable          table ;
    private final JLabel          statusBar = new JLabel( " " ) ;
    private final JPanel          previewPanel = new JPanel( new BorderLayout() ) ;
    private final JLabel          previewLabel =
            new JLabel( "Select a row to preview", SwingConstants.CENTER ) ;

    private final JMenuItem startItem = new JMenuItem( "Start processing" ) ;
    private final JMenuItem stopItem  = new JMenuItem( "Stop processing" ) ;

    private HeaderFilterController headerFilters ;
    private Controller    controller ;
    private volatile String batchProgress = "" ;

    /** Cumulative time spent processing (Start..Stop), plus the current run if active. */
    private ProcessingState procState = ProcessingState.STOPPED ;
    private long    processingAccumMs = 0 ;
    private long    processingStartedMs = 0 ;

    /** Full-resolution image currently being previewed, and its source file. */
    private BufferedImage previewImage ;
    private File          previewFile ;
    /** Monotonic token so a slow load for an old selection cannot overwrite a newer one. */
    private long          previewToken ;

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
        headerFilters = new HeaderFilterController( table ) ;
        table.getRowSorter().addRowSorterListener( e -> refreshStatusBar() ) ;

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

        table.getSelectionModel().addListSelectionListener( e -> {
            if( e.getValueIsAdjusting() ) return ;
            int viewRow = table.getSelectedRow() ;
            showPreview( viewRow < 0 ? null
                    : model.rowAt( table.convertRowIndexToModel( viewRow ) ).task ) ;
        } ) ;

        previewPanel.setBorder( BorderFactory.createTitledBorder( "Preview" ) ) ;
        previewPanel.setPreferredSize( new Dimension( 100, PREVIEW_PANEL_HEIGHT ) ) ;
        previewPanel.add( previewLabel, BorderLayout.CENTER ) ;
        previewPanel.addComponentListener( new ComponentAdapter() {
            @Override public void componentResized( ComponentEvent e ) { rescalePreview() ; }
        } ) ;

        setJMenuBar( buildMenuBar() ) ;

        setLayout( new BorderLayout() ) ;
        add( new JScrollPane( table ), BorderLayout.CENTER ) ;

        statusBar.setBorder( BorderFactory.createEmptyBorder( 4, 8, 4, 8 ) ) ;
        JPanel south = new JPanel( new BorderLayout() ) ;
        south.add( previewPanel, BorderLayout.CENTER ) ;
        south.add( statusBar, BorderLayout.SOUTH ) ;
        add( south, BorderLayout.SOUTH ) ;

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

    private JMenuBar buildMenuBar() {
        JMenu file = new JMenu( "File" ) ;

        // Enablement is driven entirely by processingStateChanged(); the engine's
        // own compareAndSet / !stopRequested guards absorb any stray double-click.
        startItem.addActionListener( a -> { if( controller != null ) controller.startProcessing() ; } ) ;
        stopItem.setEnabled( false ) ;
        stopItem.addActionListener( a -> { if( controller != null ) controller.stopProcessing() ; } ) ;
        file.add( startItem ) ;
        file.add( stopItem ) ;
        file.addSeparator() ;

        JMenuItem exit = new JMenuItem( "Exit" ) ;
        exit.addActionListener( a -> dispatchEvent( new WindowEvent( this, WindowEvent.WINDOW_CLOSING ) ) ) ;
        file.add( exit ) ;

        JMenu view = new JMenu( "View" ) ;
        JMenuItem clearFilters = new JMenuItem( "Clear all filters" ) ;
        clearFilters.addActionListener( a -> { if( headerFilters != null ) headerFilters.clearAll() ; } ) ;
        view.add( clearFilters ) ;

        JMenuBar bar = new JMenuBar() ;
        bar.add( file ) ;
        bar.add( view ) ;
        return bar ;
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

    @Override
    public void processingStateChanged( ProcessingState state ) {
        SwingUtilities.invokeLater( () -> {
            boolean wasActive = procState != ProcessingState.STOPPED ;
            boolean nowActive = state != ProcessingState.STOPPED ;
            if( nowActive && !wasActive ) {
                processingStartedMs = System.currentTimeMillis() ;
            }
            else if( !nowActive && wasActive ) {
                processingAccumMs += System.currentTimeMillis() - processingStartedMs ;
                processingStartedMs = 0 ;
            }
            procState = state ;
            startItem.setEnabled( state == ProcessingState.STOPPED ) ;
            stopItem.setEnabled( state == ProcessingState.RUNNING ) ;
            refreshStatusBar() ;
        } ) ;
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

    // ---- preview panel -----------------------------------------------

    /** Load and show the given task's image; null clears the preview. */
    private void showPreview( QTask task ) {
        File file = task == null ? null : task.getImgFile() ;
        if( Objects.equals( file, previewFile ) ) return ;

        previewFile  = file ;
        previewImage = null ;
        long token = ++previewToken ;

        previewLabel.setIcon( null ) ;
        if( file == null ) { previewLabel.setText( "Select a row to preview" ) ; return ; }
        if( !file.isFile() ) { previewLabel.setText( "Image not found" ) ; return ; }
        previewLabel.setText( "Loading…" ) ;

        new Thread( () -> {
            BufferedImage img = null ;
            try {
                img = ImageIO.read( file ) ;
            }
            catch( Exception ex ) {
                log.warn( "Could not read preview image {}", file, ex ) ;
            }
            BufferedImage loaded = img ;
            SwingUtilities.invokeLater( () -> {
                if( token != previewToken ) return ;   // a newer selection won
                previewImage = loaded ;
                if( loaded == null ) {
                    previewLabel.setIcon( null ) ;
                    previewLabel.setText( "Could not load image" ) ;
                }
                else {
                    previewLabel.setText( null ) ;
                    rescalePreview() ;
                }
            } ) ;
        }, "preview-loader" ).start() ;
    }

    /** Fit {@link #previewImage} inside the preview panel without overflow, preserving aspect ratio. */
    private void rescalePreview() {
        if( previewImage == null ) return ;

        Insets in = previewPanel.getInsets() ;
        int availW = previewPanel.getWidth()  - in.left - in.right  - 8 ;
        int availH = previewPanel.getHeight() - in.top  - in.bottom - 8 ;
        if( availW <= 0 || availH <= 0 ) return ;

        int iw = previewImage.getWidth() ;
        int ih = previewImage.getHeight() ;
        double scale = Math.min( (double) availW / iw, (double) availH / ih ) ;
        scale = Math.min( scale, 3.0 ) ;   // allow modest upscaling of narrow strips

        int w = Math.max( 1, (int) Math.round( iw * scale ) ) ;
        int h = Math.max( 1, (int) Math.round( ih * scale ) ) ;
        previewLabel.setIcon( new ImageIcon(
                previewImage.getScaledInstance( w, h, Image.SCALE_SMOOTH ) ) ) ;
        previewLabel.setText( null ) ;
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
        long procMs = processingAccumMs +
                ( processingStartedMs > 0 ? System.currentTimeMillis() - processingStartedMs : 0 ) ;
        long elapsed = procMs / 1000 ;
        int visible = table.getRowSorter() != null ? table.getRowSorter().getViewRowCount() : model.getRowCount() ;
        String text = String.format(
                "%s   |   Showing %d/%d   |   Queued %d  Running %d  Done %d  Failed %d  Skipped %d   |   Processing time %d:%02d",
                stateLabel(),
                visible, model.getRowCount(),
                c.get( TaskStatus.QUEUED ), c.get( TaskStatus.RUNNING ), c.get( TaskStatus.DONE ),
                c.get( TaskStatus.FAILED ), c.get( TaskStatus.SKIPPED ),
                elapsed / 60, elapsed % 60 ) ;
        if( !batchProgress.isEmpty() ) text += "   |   " + batchProgress ;
        statusBar.setText( text ) ;
    }

    private String stateLabel() {
        switch( procState ) {
            case RUNNING:  return "RUNNING" ;
            case STOPPING: return "SCHEDULED TO STOP" ;
            default:       return "STOPPED" ;
        }
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
