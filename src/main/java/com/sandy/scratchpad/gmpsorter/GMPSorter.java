package com.sandy.scratchpad.gmpsorter ;

import java.awt.* ;
import java.awt.event.KeyEvent ;
import java.io.File ;
import java.util.Stack ;

import javax.swing.* ;
import javax.swing.event.ListSelectionEvent ;
import javax.swing.event.ListSelectionListener ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.scratchpad.jn.imgsorter.ThumbnailViewer ;

@SuppressWarnings( "serial" )
public class GMPSorter extends JFrame implements ListSelectionListener {
    
    static final Logger log = Logger.getLogger( GMPSorter.class ) ;
    
    private static final int WIDTH = 900 ;
    
    private FileList fileList = null ;
    private TopicList topicList = null ;
    private ThumbnailViewer imgViewer = null ;
    
    private File baseDir = null ;
    
    private Stack<File> undoStack = new Stack<>() ;

    public GMPSorter( File dir ) {
        super( "GMP question sorter." ) ;
        this.baseDir = dir ;
        
        setUpUI() ;
        setUpListeners() ;
        
        setVisible( true ) ;
        
        fileList.setSelectedIndex( 0 ) ;
    }
    
    private void setUpUI() {
        
        Container contentPane = getContentPane() ;
        contentPane.setLayout( new BorderLayout() ) ;
        contentPane.add( getFrameUI(), BorderLayout.CENTER ) ;
        
        setBounds( 100, 100, WIDTH, 950 ) ;
    }
    
    private JPanel getFrameUI() {
        JPanel panel = new JPanel( new BorderLayout() ) ;
        panel.add( getImageViewer(), BorderLayout.SOUTH ) ;
        panel.add( getListPanel(), BorderLayout.CENTER ) ;
        return panel ;
    }
    
    private JPanel getListPanel() {

        this.fileList = new FileList( this ) ;
        this.topicList = new TopicList( this ) ;
        
        JPanel panel = new JPanel() ;
        panel.setLayout( new GridLayout( 1, 2 ) ) ;
        panel.add( getScrollPane( this.fileList ) ) ;
        panel.add( getScrollPane( this.topicList ) ) ;
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
        switch( key ) {
            case 'f':
                moveSelectionDown( fileList ) ;
                break ;
            case 'j':
                moveSelectionDown( topicList ) ;
                break ;
            case 'd':
                moveSelectionUp( fileList ) ;
                break ;
            case 'k':
                moveSelectionUp( topicList ) ;
                break ;
            case ' ':
                moveImageFile() ;
                break ;
            case 'z':
                undo() ;
                break ;
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
    
    private void moveImageFile() {
        File srcFile = new File( getBaseDir(), fileList.getSelectedValue() ) ;
        File destDir = new File( getBaseDir(), topicList.getSelectedValue() ) ;
        destDir = new File( destDir, "GMP 1718" ) ;
        File destFile = new File( destDir, fileList.getSelectedValue() ) ;
        
        log.debug( destFile.getAbsolutePath().substring( getBaseDir().getAbsolutePath().length() ) ) ;
        try {
            FileUtils.moveFile( srcFile, destFile ) ;
            fileList.removeSelectedValue() ;
            undoStack.push( destFile ) ;
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
    
    public static void main( String[] args ) {
        File baseDir = new File( "/home/sandeep/projects/source/SConsoleProcessedImages/IIT - Chemistry" ) ;
        new GMPSorter( baseDir ) ;
    }

}
