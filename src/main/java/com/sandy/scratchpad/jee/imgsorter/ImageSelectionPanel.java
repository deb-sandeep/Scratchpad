package com.sandy.scratchpad.jee.imgsorter;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent ;
import javax.swing.event.ListSelectionListener ;
import java.awt.*;
import java.awt.event.ActionEvent ;
import java.awt.event.ActionListener ;
import java.awt.event.KeyAdapter ;
import java.awt.event.KeyEvent ;
import java.io.File ;
import java.io.FileFilter ;
import java.util.Arrays ;
import java.util.Comparator ;
import java.util.List ;

@SuppressWarnings( "serial" )
public class ImageSelectionPanel extends JPanel 
    implements ActionListener, ListSelectionListener {

    private QBankImageSorter parent          = null ;
    private JButton          imgFolderSelBtn = new JButton( "Select folder" ) ;
    private JButton      moveImgsBtn      = new JButton( ">" ) ;
    private JButton      removeChapterBtn = new JButton( "X" ) ;
    private JFileChooser fileChooser      = new JFileChooser() ;

    private DefaultListModel<File> listModel = new DefaultListModel<>() ;
    private JList<File>            imageList = new JList<>( listModel ) ;

    private File imgFolder = null ;

    public ImageSelectionPanel( QBankImageSorter parent ) {
        this.parent = parent ;

        File imgDir = new File( System.getProperty( "user.home" ) +
                                "/Documents/StudyNotes/question-bank" ) ;
        if( imgDir.exists() ) {
            imgFolder = imgDir ;
        }

        setUpUI() ;
    }

    private void setUpUI() {
        setLayout( new BorderLayout() ) ;
        
        if( imgFolder != null ) {
            fileChooser.setCurrentDirectory( imgFolder ) ;
        }
        fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY ) ;
        fileChooser.setDialogTitle( "Select image folder" ) ;
        fileChooser.setMultiSelectionEnabled( false ) ;
        
        JPanel btnPanel = new JPanel( new BorderLayout() ) ;
        btnPanel.add( imgFolderSelBtn, BorderLayout.CENTER ) ;
        btnPanel.add( removeChapterBtn, BorderLayout.EAST ) ;
        
        JPanel transferPanel = new JPanel( new BorderLayout() ) ;
        transferPanel.add( moveImgsBtn, BorderLayout.CENTER ) ;

        imageList.setCellRenderer( new ImageListRenderer() ) ;
        
        JScrollPane sp = new JScrollPane( imageList ) ;
        add( sp, BorderLayout.CENTER ) ;
        add( btnPanel, BorderLayout.NORTH ) ;
        add( transferPanel, BorderLayout.EAST ) ;
        
        setUpEventListeners() ;
    }
    
    private void setUpEventListeners() {
        imgFolderSelBtn.addActionListener( this ) ;
        removeChapterBtn.addActionListener( this ) ;
        moveImgsBtn.addActionListener( this ) ;
        imageList.addListSelectionListener( this ) ;

        imageList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed( KeyEvent e ) {

                if( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
                    moveSelectedImages() ;
                }
            }
        } ) ;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        
        if( e.getSource() == imgFolderSelBtn ) {
            if( fileChooser.showOpenDialog( this.parent ) == JFileChooser.APPROVE_OPTION ) {
                imageFolderChanged( fileChooser.getSelectedFile() ) ;
            } ;
        }
        else if( e.getSource() == removeChapterBtn ) {
            if( imageList.getSelectedIndex() != -1 ) {
                int[] selIndices = imageList.getSelectedIndices() ;
                for( int i=0; i<selIndices.length; i++ ) {
                    listModel.remove( 0 ) ;
                }
                if( !listModel.isEmpty() ) {
                    imageList.setSelectedIndex( 0 ) ;
                }
            }
        }
        else if( e.getSource() == moveImgsBtn ) {
            moveSelectedImages() ;
        }
    }
    
    private static boolean isImageFile( File file ) {
        String name = file.getName().toLowerCase() ;
        return name.endsWith( ".png" ) ||
               name.endsWith( ".jpg" ) ||
               name.endsWith( ".jpeg" ) ;
    }

    private void imageFolderChanged( File newFolder ){

        this.imgFolder = newFolder ;
        this.listModel.clear() ;

        File[] files = newFolder.listFiles( new FileFilter() {
            public boolean accept( File file ) {
                return !file.isDirectory() && isImageFile( file ) ;
            }
        } ) ;

        if( files != null && files.length > 0 ) {
            Arrays.sort( files, new Comparator<File>() {
                public int compare( File f1, File f2 ) {
                    return f1.getName().compareTo( f2.getName() ) ;
                }
            } ) ;

            for( File file : files ) {
                this.listModel.addElement( file ) ;
            }
        }

        this.parent.rootFolderChanged( newFolder ) ;
    }
    
    private void moveSelectedImages() {

        List<File> selectedFiles = imageList.getSelectedValuesList() ;
        String moveResult = this.parent.moveFiles( selectedFiles ) ;


        if( moveResult == null ) {
            for( File file : selectedFiles ) {
                listModel.removeElement( file ) ;
            }
            
            if( !listModel.isEmpty() ) {
                imageList.setSelectedIndex( 0 ) ;
            }
        }
        else {
            JOptionPane.showMessageDialog( this.parent, moveResult ) ;
        }
    }

    @Override
    public void valueChanged( ListSelectionEvent e ) {
        if( e.getValueIsAdjusting() ) return ;
        
        File imgFile = null ;
        
        List<File> files = imageList.getSelectedValuesList() ;
        if( files != null && !files.isEmpty() ) {
            imgFile = files.get( files.size()-1 ) ;
        }
        this.parent.showThumbnail( imgFile ) ;
    }
}
