package com.sandy.scratchpad.jn.imgsorter;

import java.awt.Container ;
import java.awt.GridLayout ;
import java.io.File ;
import java.util.List ;

import javax.swing.JFrame ;

import org.apache.log4j.Logger ;

@SuppressWarnings( "serial" )
public class JNImageSorter extends JFrame {
    
    static final Logger log = Logger.getLogger( ImageSelectionPanel.class ) ;
    
    public static boolean CREATE_JN_FILE = false ;
    public static String  DEST_IMG_SUBFOLDER = "/img/pages" ;
    public static String  BOOK_NAME = null ;
    
    public static void main( String[] args ) {
        
        JNImageSorter app = new JNImageSorter() ;
        app.setVisible( true ) ;
    }

    private ImageSelectionPanel   imgSelPanel     = null ;
    private ChapterSelectionPanel chapSelPanel    = null ;
    private ThumbnailViewer       thumbnailViewer = new ThumbnailViewer() ;
    
    public JNImageSorter() {
        
        this.imgSelPanel  = new ImageSelectionPanel( this ) ;
        this.chapSelPanel = new ChapterSelectionPanel( this ) ;
        setUpUI() ;
    }
    
    private void setUpUI() {
        
        setBounds( 100, 100, 1200, 800 ) ;
        setDefaultCloseOperation( JFrame.EXIT_ON_CLOSE ) ;
        setUpUIComponents() ;
    }
    
    private void setUpUIComponents() {
        Container contentPane = super.getContentPane() ;
        contentPane.setLayout( new GridLayout( 1, 3 ) ) ;
        contentPane.add( thumbnailViewer ) ;
        contentPane.add( imgSelPanel ) ;
        contentPane.add( chapSelPanel ) ;
    }

    public String moveFiles( List<File> largeFiles,
                             boolean removeTargetFolderAfterMove ) {
        try {
            return chapSelPanel.acceptFiles( largeFiles,
                                             removeTargetFolderAfterMove ) ;
        }
        catch( Exception e ) {
            log.error( "Exception moving files.", e ) ;
            return e.getMessage() ;
        }
    }
    
    public void showThumbnail( File imgFile ) {
        thumbnailViewer.showImage( imgFile ) ;
    }
}
