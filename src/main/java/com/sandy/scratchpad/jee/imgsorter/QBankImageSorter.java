package com.sandy.scratchpad.jee.imgsorter;

import org.apache.log4j.Logger ;

import javax.swing.*;
import java.awt.*;
import java.io.File ;
import java.util.List ;

@SuppressWarnings( "serial" )
public class QBankImageSorter extends JFrame {
    
    static final Logger log = Logger.getLogger( ImageSelectionPanel.class ) ;

    public static void main( String[] args ) {
        
        QBankImageSorter app = new QBankImageSorter() ;
        app.setVisible( true ) ;
    }

    private ImageSelectionPanel   imgSelPanel     = null ;
    private ChapterSelectionPanel chapSelPanel    = null ;
    private ThumbnailViewer       thumbnailViewer = new ThumbnailViewer() ;
    
    public QBankImageSorter() {
        
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

    public String moveFiles( List<File> largeFiles ) {
        try {
            return chapSelPanel.acceptFiles( largeFiles ) ;
        }
        catch( Exception e ) {
            log.error( "Exception moving files.", e ) ;
            return e.getMessage() ;
        }
    }
    
    public void showThumbnail( File imgFile ) {
        thumbnailViewer.showImage( imgFile ) ;
    }

    public void rootFolderChanged( File root ) {
        chapSelPanel.loadChapters( root ) ;
    }
}
