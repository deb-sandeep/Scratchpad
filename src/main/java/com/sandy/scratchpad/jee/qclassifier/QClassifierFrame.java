package com.sandy.scratchpad.jee.qclassifier;

import com.sandy.scratchpad.jee.qclassifier.QTaskTreeTableModel.Node;
import com.sandy.scratchpad.jee.qclassifier.QTaskTreeTableModel.PartNode;
import com.sandy.scratchpad.jee.qclassifier.QTaskTreeTableModel.QuestionNode;
import com.sandy.scratchpad.jee.qclassifier.QTaskTreeTableModel.SourceNode;
import lombok.extern.slf4j.Slf4j;

import javax.imageio.ImageIO;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import static com.sandy.scratchpad.jee.qclassifier.QTaskTreeTableModel.*;

/**
 * Live monitor for {@link QImgClassifier}, shown as a tree table: one node per
 * source, one row per question beneath it, and the part images beneath a
 * multi-part question (collapsed by default). Question rows move
 * QUEUED -> RUNNING -> DONE/FAILED (or start SKIPPED if already classified).
 * Click the triangle (or use Left / Right) to collapse / expand; double-click a
 * question or part to open its image; right-click offers open / show in
 * QImgExtractor (⌘E) / re-queue.
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

    private static final int   PREVIEW_PANEL_HEIGHT = 300 ;
    private static final int   INDENT      = 18 ;
    private static final int   LEFT_PAD    = 4 ;
    private static final int   EXPANDER_W  = 12 ;
    private static final int   ICON_GAP    = 4 ;
    private static final Color SOURCE_BG   = new Color( 0xE3E8EF ) ;
    /** ⌘E on macOS, Ctrl+E elsewhere. */
    private static final KeyStroke SHOW_IN_EXTRACTOR_KEY = KeyStroke.getKeyStroke(
            java.awt.event.KeyEvent.VK_E, Toolkit.getDefaultToolkit().getMenuShortcutKeyMaskEx() ) ;

    private final QTaskTreeTableModel model = new QTaskTreeTableModel() ;
    private final QImgExtractorClient extractorClient = new QImgExtractorClient() ;
    private final JTable              table ;
    private final JLabel              statusBar = new JLabel( " " ) ;
    private final JPanel              previewPanel = new JPanel( new BorderLayout() ) ;
    private final JLabel              previewLabel =
            new JLabel( "Select a row to preview", SwingConstants.CENTER ) ;

    private final JMenuItem startItem = new JMenuItem( "Start processing" ) ;
    private final JMenuItem stopItem  = new JMenuItem( "Stop processing" ) ;

    /** Coalesces bursts of structural changes (discovery, status moves under a filter) into one rebuild. */
    private final Timer rebuildTimer = new Timer( 150, e -> rebuildTree() ) ;
    /** Set while a rebuild restores the selection, so the preview isn't reloaded. */
    private boolean restoringSelection ;

    private HeaderFilterController headerFilters ;
    private Controller    controller ;
    private volatile String batchProgress = "" ;

    /** Cumulative time spent processing (Start..Stop), plus the current run if active. */
    private ProcessingState procState = ProcessingState.STOPPED ;
    private long    processingAccumMs = 0 ;
    private long    processingStartedMs = 0 ;

    /** Full-resolution image currently being previewed, and the file(s) it was built from. */
    private BufferedImage previewImage ;
    private List<File>    previewFiles ;
    /** Monotonic token so a slow load for an old selection cannot overwrite a newer one. */
    private long          previewToken ;

    public QClassifierFrame() {
        super( "Gemma Question Classifier" ) ;

        table = new JTable( model ) {
            @Override
            public Component prepareRenderer( javax.swing.table.TableCellRenderer r, int row, int col ) {
                Component c = super.prepareRenderer( r, row, col ) ;
                Node n = model.nodeAt( row ) ;
                if( n instanceof SourceNode ) c.setFont( c.getFont().deriveFont( Font.BOLD ) ) ;
                if( !isRowSelected( row ) ) c.setBackground( backgroundFor( n ) ) ;
                return c ;
            }
        } ;
        table.setFillsViewportHeight( true ) ;
        table.setSelectionMode( ListSelectionModel.SINGLE_SELECTION ) ;
        rebuildTimer.setRepeats( false ) ;
        headerFilters = new HeaderFilterController( table, model, this::rebuildTree ) ;

        DefaultTableCellRenderer center = new DefaultTableCellRenderer() ;
        center.setHorizontalAlignment( SwingConstants.CENTER ) ;
        for( int c : new int[]{ COL_SUBJ, COL_STATUS, COL_ATTEMPTS, COL_TIME } ) {
            table.getColumnModel().getColumn( c ).setCellRenderer( center ) ;
        }
        table.getColumnModel().getColumn( COL_NAME ).setCellRenderer( new TreeCellRenderer() ) ;
        int[] widths = { 300, 45, 80, 70, 70, 430 } ;
        for( int i = 0 ; i < widths.length ; i++ ) {
            table.getColumnModel().getColumn( i ).setPreferredWidth( widths[i] ) ;
        }

        installMouseHandlers() ;
        installKeyBindings() ;

        table.getSelectionModel().addListSelectionListener( e -> {
            if( e.getValueIsAdjusting() || restoringSelection ) return ;
            showPreview( selectedNode() ) ;
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
        JMenuItem expandAll = new JMenuItem( "Expand all sources" ) ;
        expandAll.addActionListener( a -> { model.setAllSourcesExpanded( true ) ; rebuildTree() ; } ) ;
        view.add( expandAll ) ;
        JMenuItem collapseAll = new JMenuItem( "Collapse all sources" ) ;
        collapseAll.addActionListener( a -> { model.setAllSourcesExpanded( false ) ; rebuildTree() ; } ) ;
        view.add( collapseAll ) ;
        view.addSeparator() ;
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
        SwingUtilities.invokeLater( () -> { model.discovered( task, alreadyClassified ) ; scheduleRebuild() ; } ) ;
    }

    @Override
    public void taskStarted( QTask task ) {
        SwingUtilities.invokeLater( () -> { model.started( task ) ; afterStatusChange() ; } ) ;
    }

    @Override
    public void taskFinished( QTask task, TaskStatus status, int attempts, String result ) {
        SwingUtilities.invokeLater( () -> { model.finished( task, status, attempts, result ) ; afterStatusChange() ; } ) ;
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

    // ---- tree view maintenance ----------------------------------------

    private void afterStatusChange() {
        // A status change can move a row under an active filter / sort.
        if( model.isFilteredOrSorted() ) scheduleRebuild() ;
        refreshStatusBar() ;
    }

    private void scheduleRebuild() {
        if( !rebuildTimer.isRunning() ) rebuildTimer.start() ;
    }

    /** Rebuild the flattened view, keeping the selected node selected if it is still visible. */
    private void rebuildTree() {
        rebuildTimer.stop() ;
        Node selected = selectedNode() ;
        restoringSelection = true ;
        try {
            model.rebuild() ;
            if( selected != null && selected.viewIndex >= 0 ) {
                table.setRowSelectionInterval( selected.viewIndex, selected.viewIndex ) ;
            }
        }
        finally {
            restoringSelection = false ;
        }
        if( selected != null && selected.viewIndex < 0 ) showPreview( null ) ;
        refreshStatusBar() ;
    }

    private Node selectedNode() {
        int viewRow = table.getSelectedRow() ;
        return viewRow < 0 ? null : model.nodeAt( viewRow ) ;
    }

    private void toggle( Node n ) {
        model.setExpanded( n, !n.expanded ) ;
    }

    // ---- UI plumbing ---------------------------------------------------

    private void installMouseHandlers() {
        table.addMouseListener( new MouseAdapter() {
            @Override public void mousePressed( MouseEvent e ) {
                if( SwingUtilities.isLeftMouseButton( e ) && e.getClickCount() == 1 ) {
                    Node n = nodeUnder( e ) ;
                    if( n != null && onExpander( e, n ) ) toggle( n ) ;
                }
                maybePopup( e ) ;
            }
            @Override public void mouseReleased( MouseEvent e ) { maybePopup( e ) ; }
            @Override public void mouseClicked( MouseEvent e ) {
                if( e.getClickCount() != 2 || !SwingUtilities.isLeftMouseButton( e ) ) return ;
                Node n = nodeUnder( e ) ;
                if( n == null || onExpander( e, n ) ) return ;
                if( n instanceof SourceNode ) toggle( n ) ;
                else                          openImage( n ) ;
            }
        } ) ;
    }

    private void installKeyBindings() {
        InputMap  im = table.getInputMap( JComponent.WHEN_ANCESTOR_OF_FOCUSED_COMPONENT ) ;
        ActionMap am = table.getActionMap() ;
        table.getInputMap( JComponent.WHEN_IN_FOCUSED_WINDOW ).put( SHOW_IN_EXTRACTOR_KEY, "qc-show-in-extractor" ) ;
        am.put( "qc-show-in-extractor", new AbstractAction() {
            @Override public void actionPerformed( ActionEvent e ) {
                Node n = selectedNode() ;
                if( n != null ) showInExtractor( n ) ;
            }
        } ) ;
        im.put( KeyStroke.getKeyStroke( "RIGHT" ), "qc-expand" ) ;
        im.put( KeyStroke.getKeyStroke( "LEFT" ),  "qc-collapse" ) ;
        am.put( "qc-expand", new AbstractAction() {
            @Override public void actionPerformed( ActionEvent e ) {
                Node n = selectedNode() ;
                if( n != null ) model.setExpanded( n, true ) ;
            }
        } ) ;
        am.put( "qc-collapse", new AbstractAction() {
            @Override public void actionPerformed( ActionEvent e ) {
                Node n = selectedNode() ;
                if( n == null ) return ;
                if( n.isExpandable() && n.expanded ) { model.setExpanded( n, false ) ; return ; }
                Node parent = parentOf( n ) ;
                if( parent != null && parent.viewIndex >= 0 ) {
                    table.setRowSelectionInterval( parent.viewIndex, parent.viewIndex ) ;
                    table.scrollRectToVisible( table.getCellRect( parent.viewIndex, 0, true ) ) ;
                }
            }
        } ) ;
    }

    private static Node parentOf( Node n ) {
        if( n instanceof QuestionNode ) return ( (QuestionNode) n ).parent ;
        if( n instanceof PartNode )     return ( (PartNode) n ).parent ;
        return null ;
    }

    /** True if the event is on the expand / collapse triangle of an expandable node. */
    private boolean onExpander( MouseEvent e, Node n ) {
        if( !n.isExpandable() ) return false ;
        int viewCol = table.columnAtPoint( e.getPoint() ) ;
        if( viewCol < 0 || table.convertColumnIndexToModel( viewCol ) != COL_NAME ) return false ;
        int x = e.getX() - table.getCellRect( n.viewIndex, viewCol, false ).x ;
        int start = LEFT_PAD + n.depth() * INDENT ;
        return x >= start - 2 && x <= start + EXPANDER_W + ICON_GAP ;
    }

    private void maybePopup( MouseEvent e ) {
        if( !e.isPopupTrigger() ) return ;
        int viewRow = table.rowAtPoint( e.getPoint() ) ;
        if( viewRow < 0 ) return ;
        table.setRowSelectionInterval( viewRow, viewRow ) ;
        Node n = model.nodeAt( viewRow ) ;

        JPopupMenu menu = new JPopupMenu() ;
        if( !( n instanceof SourceNode ) ) {
            JMenuItem open = new JMenuItem( "Open image" ) ;
            open.addActionListener( a -> openImage( n ) ) ;
            menu.add( open ) ;

            JMenuItem extractor = new JMenuItem( "Show in QImgExtractor" ) ;
            extractor.setAccelerator( SHOW_IN_EXTRACTOR_KEY ) ;
            extractor.addActionListener( a -> showInExtractor( n ) ) ;
            menu.add( extractor ) ;
        }
        if( n instanceof QuestionNode ) {
            QuestionNode q = (QuestionNode) n ;
            JMenuItem requeue = new JMenuItem( "Re-queue" ) ;
            boolean canRequeue = controller != null &&
                    ( q.status == TaskStatus.DONE || q.status == TaskStatus.FAILED
                            || q.status == TaskStatus.SKIPPED ) ;
            requeue.setEnabled( canRequeue ) ;
            requeue.addActionListener( a -> controller.reprocess( q.task ) ) ;
            menu.add( requeue ) ;
        }
        if( n.isExpandable() ) {
            if( menu.getComponentCount() > 0 ) menu.addSeparator() ;
            JMenuItem toggle = new JMenuItem( n.expanded ? "Collapse" : "Expand" ) ;
            toggle.addActionListener( a -> toggle( n ) ) ;
            menu.add( toggle ) ;
        }

        menu.show( table, e.getX(), e.getY() ) ;
    }

    private Node nodeUnder( MouseEvent e ) {
        int viewRow = table.rowAtPoint( e.getPoint() ) ;
        return viewRow < 0 ? null : model.nodeAt( viewRow ) ;
    }

    /** Image file(s) a node represents: a question's image or parts, a single part, or none for a source. */
    private static List<File> imageFilesOf( Node n ) {
        if( n instanceof QuestionNode ) return ( (QuestionNode) n ).task.getImgFiles() ;
        if( n instanceof PartNode )     return List.of( ( (PartNode) n ).file ) ;
        return null ;
    }

    // ---- preview panel -----------------------------------------------

    /** Load and show the node's image (collating multi-part questions); null / a source clears it. */
    private void showPreview( Node node ) {
        List<File> files = node == null ? null : imageFilesOf( node ) ;
        if( Objects.equals( files, previewFiles ) ) return ;

        previewFiles = files ;
        previewImage = null ;
        long token = ++previewToken ;

        previewLabel.setIcon( null ) ;
        if( files == null ) { previewLabel.setText( "Select a question to preview" ) ; return ; }
        if( !files.stream().allMatch( File::isFile ) ) { previewLabel.setText( "Image not found" ) ; return ; }
        previewLabel.setText( "Loading…" ) ;

        new Thread( () -> {
            BufferedImage img = null ;
            try {
                img = ImageCollator.collate( files ) ;
            }
            catch( Exception ex ) {
                log.warn( "Could not read preview image {}", files, ex ) ;
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

    /**
     * Ask QImgExtractor to open the image's project and scroll to it, for repair.
     * A multi-part question is shown by its first part; errors are reported in a dialog.
     */
    private void showInExtractor( Node n ) {
        List<File> files = imageFilesOf( n ) ;
        if( files == null ) return ;
        File img = files.get( 0 ) ;
        extractorClient.showQuestionImage( img ).thenAccept( error -> {
            if( error == null ) return ;
            SwingUtilities.invokeLater( () -> JOptionPane.showMessageDialog( this,
                    "Could not show " + img.getName() + " in QImgExtractor:\n" + error,
                    "QImgExtractor", JOptionPane.WARNING_MESSAGE ) ) ;
        } ) ;
    }

    /** Open the node's image; a multi-part question is collated into a temp PNG first. */
    private void openImage( Node n ) {
        List<File> files = imageFilesOf( n ) ;
        if( files == null ) return ;
        try {
            File target = files.get( 0 ) ;
            if( files.size() > 1 ) {
                String id = ( (QuestionNode) n ).task.getQuestionId().replaceAll( "[^A-Za-z0-9_-]", "_" ) ;
                target = File.createTempFile( "qclassifier-" + id + "-", ".png" ) ;
                target.deleteOnExit() ;
                ImageIO.write( ImageCollator.collate( files ), "png", target ) ;
            }
            Desktop.getDesktop().open( target ) ;
        }
        catch( Exception ex ) {
            log.error( "Could not open image {}", files, ex ) ;
            JOptionPane.showMessageDialog( this, "Could not open image:\n" + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE ) ;
        }
    }

    private void refreshStatusBar() {
        Map<TaskStatus,Integer> c = model.statusCounts() ;
        long procMs = processingAccumMs +
                ( processingStartedMs > 0 ? System.currentTimeMillis() - processingStartedMs : 0 ) ;
        long elapsed = procMs / 1000 ;
        String text = String.format(
                "%s   |   %d sources   |   Showing %d/%d questions   |   Queued %d  Running %d  Done %d  Failed %d  Skipped %d   |   Processing time %d:%02d",
                stateLabel(),
                model.getSourceCount(),
                model.getMatchingQuestionCount(), model.getQuestionCount(),
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

    private static Color backgroundFor( Node n ) {
        if( n instanceof SourceNode ) return SOURCE_BG ;
        if( n instanceof PartNode )   return colorFor( ( (PartNode) n ).parent.status ) ;
        return colorFor( ( (QuestionNode) n ).status ) ;
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

    // ---- tree column rendering ---------------------------------------

    /** Name column: indentation by depth plus an expand / collapse triangle for expandable nodes. */
    private class TreeCellRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent( JTable t, Object value, boolean sel,
                                                        boolean foc, int row, int col ) {
            super.getTableCellRendererComponent( t, value, sel, foc, row, col ) ;
            Node n = model.nodeAt( row ) ;
            setText( n.label() ) ;
            setIcon( new ExpanderIcon( n.isExpandable(), n.expanded ) ) ;
            setIconTextGap( ICON_GAP ) ;
            setBorder( BorderFactory.createEmptyBorder( 0, LEFT_PAD + n.depth() * INDENT, 0, 0 ) ) ;
            return this ;
        }
    }

    /** Fixed-width triangle (▸ collapsed, ▾ expanded); blank for leaves so labels stay aligned. */
    private static final class ExpanderIcon implements Icon {
        private final boolean expandable ;
        private final boolean expanded ;

        ExpanderIcon( boolean expandable, boolean expanded ) {
            this.expandable = expandable ;
            this.expanded   = expanded ;
        }

        @Override public int getIconWidth()  { return EXPANDER_W ; }
        @Override public int getIconHeight() { return EXPANDER_W ; }

        @Override
        public void paintIcon( Component c, Graphics g, int x, int y ) {
            if( !expandable ) return ;
            Graphics2D g2 = (Graphics2D) g.create() ;
            try {
                g2.setRenderingHint( RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON ) ;
                g2.setColor( Color.DARK_GRAY ) ;
                int s = EXPANDER_W ;
                Polygon p = expanded
                        ? new Polygon( new int[]{ x + 2, x + s - 2, x + s / 2 }, new int[]{ y + 3, y + 3, y + s - 3 }, 3 )
                        : new Polygon( new int[]{ x + 3, x + s - 3, x + 3 }, new int[]{ y + 2, y + s / 2, y + s - 2 }, 3 ) ;
                g2.fillPolygon( p ) ;
            }
            finally {
                g2.dispose() ;
            }
        }
    }
}
