package com.sandy.scratchpad.gmpsorter ;

import java.awt.* ;
import java.awt.event.KeyEvent ;
import java.io.File ;
import java.util.Stack ;
import java.util.concurrent.ArrayBlockingQueue ;

import javax.swing.* ;
import javax.swing.event.ListSelectionEvent ;
import javax.swing.event.ListSelectionListener ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.scratchpad.jn.imgsorter.ThumbnailViewer ;

@SuppressWarnings( "serial" )
public class GMPSorter extends JFrame implements ListSelectionListener {
    
    public static final String BOOK_SHORT_NAME = "YG File 1 - 2020" ;
    public static final String SUBJECT_FOLDER_NAME = "IIT - Chemistry" ;
    public static final String IMG_PREFIX = "Chem_Q_" ;
    
    private class TopicShortcutProcessor extends Thread {
        
        private ArrayBlockingQueue<Integer> keyStrokes = new ArrayBlockingQueue<>( 10 ) ;
        
        public TopicShortcutProcessor() {
            setDaemon( true ) ;
        }
        
        public void run() {
            while( true ) {
                try {
                    // This call will block till the user presses a numeric key
                    Integer k1 = keyStrokes.take() ;
                    
                    // Once the user presses a key, there is a possibility that 
                    // he might want to enter a two digit number. To cater for
                    // that possibility, we sleep for some time, wake up to see
                    // if the queue has more key strokes. If so, we assemble the 
                    // number else we proceed with the first key.
                    Thread.sleep( 250 ) ;
                    
                    int index = 0 ;
                    if( keyStrokes.isEmpty() ) {
                        index = k1-1 ;
                    }
                    else {
                        Integer k2 = keyStrokes.take() ;
                        index = ( k1*10 + k2 ) - 1 ;
                        keyStrokes.clear() ;
                    }
                    
//                    topicList.setSelectedIndex( index ) ;
                }
                catch( InterruptedException e ) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    static final Logger log = Logger.getLogger( GMPSorter.class ) ;
    static final Font LIST_FONT = new Font( "Arial", Font.PLAIN, 14 ) ;
    
    private static final int WIDTH = 900 ;
    
    private FileList fileList = null ;
//    private TopicList topicList = null ;
    private TopicButtonPanel topicBtnPanel = null ;
    private ThumbnailViewer imgViewer = null ;
    
    private File baseDir = null ;
    
    private Stack<File> undoStack = new Stack<>() ;
    private TopicShortcutProcessor tsProcessor = new TopicShortcutProcessor() ;

    public GMPSorter( File dir ) {
        super( "GMP question sorter." ) ;
        this.baseDir = dir ;
        
        setUpUI() ;
        setUpListeners() ;
        
        setVisible( true ) ;
        
        imgViewer.requestFocus() ;
        fileList.setSelectedIndex( 0 ) ;
        tsProcessor.start() ;
    }
    
    private void setUpUI() {
        
        Container contentPane = getContentPane() ;
        contentPane.setLayout( new BorderLayout() ) ;
        contentPane.add( getFrameUI(), BorderLayout.CENTER ) ;
        
        setBounds( 100, 100, WIDTH, 950 ) ;
    }
    
    private JPanel getFrameUI() {
        JPanel panel = new JPanel( new BorderLayout() ) ;
        panel.add( getImageViewer(), BorderLayout.NORTH ) ;
        panel.add( getListPanel(), BorderLayout.CENTER ) ;
        return panel ;
    }
    
    private JPanel getListPanel() {

        this.fileList = new FileList( this ) ;
        this.fileList.setFont( LIST_FONT );
        
//        this.topicList = new TopicList( this ) ;
//        this.topicList.setFont( LIST_FONT );

        this.topicBtnPanel = new TopicButtonPanel( this ) ;
        
        
        JPanel panel = new JPanel() ;
        panel.setLayout( new BorderLayout() ) ;
        panel.add( getScrollPane( this.fileList ), BorderLayout.WEST ) ;
        panel.add( this.topicBtnPanel, BorderLayout.CENTER ) ;
        return panel ;
    }
    
    private JComponent getImageViewer() {
        
        this.imgViewer = new ThumbnailViewer() ;
        
        this.imgViewer.setPreferredSize( new Dimension( WIDTH, 400 ) ) ;
        return this.imgViewer ;
    }
    
    private JScrollPane getScrollPane( JList<String> list ) {
        JScrollPane pane = new JScrollPane( list ) ;
        pane.setHorizontalScrollBarPolicy( JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED ) ;
        pane.setVerticalScrollBarPolicy( JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED ) ;
        return pane ;
    }
    
    private void setUpListeners() {
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE ) ;
        
        KeyEventDispatcher keyEventDispatcher = new KeyEventDispatcher() {
            public boolean dispatchKeyEvent( final KeyEvent e ) {
                if( e.getID() == KeyEvent.KEY_TYPED ) {
                    processKeyStroke( e.getKeyChar()  ) ;
                }
                return false ;
            }
        } ;
        KeyboardFocusManager.getCurrentKeyboardFocusManager()
                            .addKeyEventDispatcher( keyEventDispatcher ) ;
    }
    
    @Override
    public void valueChanged( ListSelectionEvent e ) {
        if( !e.getValueIsAdjusting() ){
            FileList list = ( FileList )e.getSource() ;
            if( list.getSelectedValue() != null ) {
                File imgFile = new File( getBaseDir(), list.getSelectedValue() ) ;
                this.imgViewer.showImage( imgFile ) ;
            }
        }
    }
    
    public File getBaseDir() {
        return this.baseDir ;
    }
    
    private void processKeyStroke( char key ) {
        if( key >= '0' && key <= '9' ) {
            tsProcessor.keyStrokes.offer( key - '0' ) ;
        }
        else {
            switch( key ) {
                case 'f':
                    moveSelectionDown( fileList ) ;
                    break ;
                case 'j':
//                    moveSelectionDown( topicList ) ;
                    break ;
                case 'd':
                    moveSelectionUp( fileList ) ;
                    break ;
                case 'k':
//                    moveSelectionUp( topicList ) ;
                    break ;
                case ' ':
                    moveImageFile() ;
                    break ;
                case 'z':
                    undo() ;
                    break ;
                default:
//                    topicList.selectValueWithFirstLetter( key ) ;
                    break ;
            }
        }
    }
    
    private void moveSelectionUp( JList<String> list ) {
        int selIndex = list.getSelectedIndex() ;
        if( selIndex > 0 ) {
            list.setSelectedIndex( --selIndex ) ;
        }
        else {
            if( list.getModel().getSize() > 0 ) {
                list.setSelectedIndex( list.getModel().getSize()-1 ) ;
            }
        }
    }
    
    private void moveSelectionDown( JList<String> list ) {
        int selIndex = list.getSelectedIndex() ;
        if( selIndex < list.getModel().getSize()-1 ) {
            list.setSelectedIndex( ++selIndex ) ;
        }
        else {
            if( list.getModel().getSize() > 0 ) {
                list.setSelectedIndex( 0 ) ;
            }
        }
    }
    
    void moveImageFile() {
        File srcFile = new File( getBaseDir(), fileList.getSelectedValue() ) ;
        File destDir = new File( getBaseDir(), topicBtnPanel.getSelectedDirName() ) ;
        destDir = new File( destDir, BOOK_SHORT_NAME ) ;
        File destFile = new File( destDir, fileList.getSelectedValue() ) ;
        
        log.debug( destFile.getAbsolutePath()
                           .substring( getBaseDir().getAbsolutePath()
                                                   .length() ) ) ;
        try {
            FileUtils.moveFile( srcFile, destFile ) ;
            fileList.removeSelectedValue() ;
            undoStack.push( destFile ) ;
            setTitle( "Num files left = " + fileList.getNumFiles() ) ;
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }
    
    private void undo() {
        
        if( !undoStack.isEmpty() ) {
            File lastMovedFile = undoStack.pop() ;
            File destFile = new File( getBaseDir(), lastMovedFile.getName() ) ;
            
            try {
                FileUtils.moveFile( lastMovedFile, destFile ) ;
                fileList.insert( lastMovedFile.getName() ) ;
            }
            catch( Exception e ) {
                e.printStackTrace();
            }
        }
    }
    
    void nextImage() {
        moveSelectionDown( fileList ) ;
    }
    
    public static void main( String[] args ) {
        File baseDir = new File( "/home/sandeep/projects/source/SConsoleProcessedImages/", 
                                 SUBJECT_FOLDER_NAME ) ;
        new GMPSorter( baseDir ) ;
    }

}
