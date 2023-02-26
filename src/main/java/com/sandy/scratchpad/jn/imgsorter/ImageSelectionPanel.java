package com.sandy.scratchpad.jn.imgsorter;

import java.awt.BorderLayout ;
import java.awt.event.* ;
import java.io.File ;
import java.io.FileFilter ;
import java.util.* ;

import javax.swing.* ;
import javax.swing.event.ListSelectionEvent ;
import javax.swing.event.ListSelectionListener ;

import org.apache.log4j.Logger ;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK ;

@SuppressWarnings( "serial" )
public class ImageSelectionPanel extends JPanel 
    implements ActionListener, ListSelectionListener {
    
    static final Logger log = Logger.getLogger( ImageSelectionPanel.class ) ;
    
    private JNImageSorter   parent          = null ;
    private JButton         imgFolderSelBtn = new JButton( "Select image folder" ) ;
    // A move action removes the target folder from the list, while a file 
    // image stores the images in the target folder without removing the folder
    // from the list.
    private JButton         moveImgsBtn     = new JButton( ">" ) ;
    private JButton         fileImgsBtn     = new JButton( ">>" ) ;
    private JButton         removeChapterBtn= new JButton( "X" ) ;
    private JFileChooser    fileChooser     = new JFileChooser() ;
    
    private DefaultListModel<String> listModel = new DefaultListModel<>() ;
    private JList<String>            imageList = new JList<>( listModel ) ;
    
    private File imgFolder = null ;
    
    public ImageSelectionPanel( JNImageSorter parent ) {
        this.parent = parent ;

        File imgDir = new File( "/home/sandeep/temp/scans/output" ) ;
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
        transferPanel.add( fileImgsBtn, BorderLayout.NORTH ) ;
        
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
        fileImgsBtn.addActionListener( this ) ;
        imageList.addListSelectionListener( this ) ;
        
        imageList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed( KeyEvent e ) {
                
                if( e.getKeyCode() == KeyEvent.VK_RIGHT ) {
                    
                    int modifiersEx = e.getModifiersEx() ;
                    if( ( modifiersEx & SHIFT_DOWN_MASK ) != 0 ) {
                        moveSelectedImages( false ) ;
                    }
                    else {
                        moveSelectedImages( true ) ;
                    }
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
            moveSelectedImages( true ) ;
        }
        else if( e.getSource() == fileImgsBtn ) {
            moveSelectedImages( false ) ;
        }
    }
    
    private void imageFolderChanged( File newFolder ) {
        
        this.imgFolder = newFolder ;
        imageList.removeAll() ;
        
        File[] files = this.imgFolder.listFiles( new FileFilter() {
            public boolean accept( File file ) {
                if( file.getName().endsWith( ".png" ) ) {
                    return true ;
                }
                return false ;
            }
        } ) ;
        
        if( files != null && files.length > 0 ) {
            DefaultListModel<String> model = ( DefaultListModel<String> )imageList.getModel() ;
            Arrays.sort( files, new Comparator<File>() {
                public int compare( File f1, File f2 ) {
                    int pgNum1 = getPageNum( f1 ) ;
                    int pgNum2 = getPageNum( f2 ) ;
                    
                    return pgNum1 - pgNum2 ;
                }
            } ) ;
            for( File file : files ) {
                model.addElement( file.getName() ) ;
            }
        }
    }
    
    private int getPageNum( File f ) {
        String fName = f.getName() ;
        String pageNum = fName.substring( 0, fName.length()-4 )
                              .substring( fName.lastIndexOf( '_' ) + 1 ) ;
        
        return Integer.parseInt( pageNum ) ;
    }
    
    private void moveSelectedImages( boolean removeTargetFolderAfterMove ) {
        
        List<String> selectedFiles = imageList.getSelectedValuesList() ;
        List<File> imgFiles = new ArrayList<>() ;
        
        for( String name : selectedFiles ) {
            imgFiles.add( new File( this.imgFolder, name ) ) ;
        }
        
        String moveResult = this.parent.moveFiles( imgFiles, 
                                                   removeTargetFolderAfterMove ) ;
        
        if( moveResult == null ) {
            for( String name : selectedFiles ) {
                listModel.removeElement( name ) ;
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
        
        List<String> files = imageList.getSelectedValuesList() ;
        if( files != null && !files.isEmpty() ) {
            imgFile = new File( this.imgFolder, files.get( files.size()-1 ) ) ;
        }
        
        this.parent.showThumbnail( imgFile ) ;
    }
}
