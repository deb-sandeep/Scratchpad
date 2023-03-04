package com.sandy.scratchpad.jn.imgsorter;

import static java.awt.event.InputEvent.SHIFT_DOWN_MASK ;

import java.awt.BorderLayout ;
import java.awt.event.ActionEvent ;
import java.awt.event.ActionListener ;
import java.awt.event.KeyAdapter ;
import java.awt.event.KeyEvent ;
import java.awt.image.BufferedImage ;
import java.io.File ;
import java.io.IOException ;
import java.util.ArrayList ;
import java.util.Arrays ;
import java.util.Comparator ;
import java.util.List ;

import javax.imageio.ImageIO ;
import javax.swing.DefaultListModel ;
import javax.swing.JButton ;
import javax.swing.JFileChooser ;
import javax.swing.JList ;
import javax.swing.JOptionPane ;
import javax.swing.JPanel ;
import javax.swing.JScrollPane ;
import javax.swing.event.ListSelectionEvent ;
import javax.swing.event.ListSelectionListener ;

import org.apache.commons.lang.math.NumberUtils ;
import org.apache.log4j.Logger ;

@SuppressWarnings( "serial" )
public class ImageSelectionPanel extends JPanel 
    implements ActionListener, ListSelectionListener {
    
    static final Logger log = Logger.getLogger( ImageSelectionPanel.class ) ;
    
    private JNImageSorter parent          = null ;
    private JButton       imgFolderSelBtn = new JButton( "Select image folder" ) ;
    // A move action removes the target folder from the list, while a file 
    // image stores the images in the target folder without removing the folder
    // from the list.
    private JButton      moveImgsBtn      = new JButton( ">" ) ;
    private JButton      fileImgsBtn      = new JButton( ">>" ) ;
    private JButton      removeChapterBtn = new JButton( "X" ) ;
    private JFileChooser fileChooser      = new JFileChooser() ;
    
    private DefaultListModel<File> listModel = new DefaultListModel<>() ;
    private JList<File>            imageList = new JList<>( listModel ) ;
    
    private File imgFolder = null ;
    
    private int numDots = 0 ;
    
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
    
    private List<File> collectPageImageFiles( File root, List<File> collectedFiles ) {
        
        File[] files = root.listFiles() ;
        Arrays.sort( files, new Comparator<File>() {
            public int compare( File f1, File f2 ) {
                return f1.getAbsolutePath().compareTo( f2.getAbsolutePath() ) ;
            }
        } ) ;
        
        for( File f : files ) {
            if( f.isDirectory() && !f.getName().equals( "hi-res" ) )  {
                collectPageImageFiles( f, collectedFiles ) ;
            }
        }
        
        for( File f : files ) {
            if( !f.isDirectory() )  {
                String fName = f.getName() ;
                if( fName.endsWith( ".png" ) ) {
                    fName = fName.substring( 0, fName.length()-4 ) ;
                    if( fName.contains( "_" ) ) {
                        
                        String fNameParts[] = fName.split( "_" ) ;
                        
                        if( NumberUtils.isDigits( fNameParts[fNameParts.length-1] ) ) {
                            
                            if( possiblePageImage( f ) ) {
                                collectedFiles.add( f ) ;
                                System.out.print( "." ) ;

                                this.numDots++ ;
                                if( this.numDots % 50 == 0 ) {
                                    System.out.println() ;
                                }
                            }
                            
                        }
                    }
                }
            }
        }
        
        return collectedFiles ;
    }
    
    private void imageFolderChanged( File newFolder ){
        
        this.imgFolder = newFolder ;
        this.listModel.clear() ;
        this.numDots = 0 ;
        
        List<File> collectedFiles = collectPageImageFiles( this.imgFolder, new ArrayList<File>() ) ;
        
        if( collectedFiles != null && !collectedFiles.isEmpty() ) {
            for( File file : collectedFiles ) {
                this.listModel.addElement( file ) ;
            }
        }
    }
    
    private void moveSelectedImages( boolean removeTargetFolderAfterMove ) {
        
        List<File> selectedFiles = imageList.getSelectedValuesList() ;
        String moveResult = this.parent.moveFiles( selectedFiles, 
                                                   removeTargetFolderAfterMove ) ;
        
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
    
    private boolean possiblePageImage( File file ) {
        
        try {
            BufferedImage img = ImageIO.read( file ) ;
            return img.getHeight() > 1000 && img.getWidth() > 800 ;
        }
        catch( IOException e ) {
            log.debug( "Possibly not an image. " + file.getAbsolutePath() ) ;
        }
        return false ;
    }
}
