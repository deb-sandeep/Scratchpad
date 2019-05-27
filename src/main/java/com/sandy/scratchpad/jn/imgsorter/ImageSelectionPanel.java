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

@SuppressWarnings( "serial" )
public class ImageSelectionPanel extends JPanel 
    implements ActionListener, ListSelectionListener {
    
    static final Logger log = Logger.getLogger( ImageSelectionPanel.class ) ;
    
    private JNImageSorter   parent          = null ;
    private JButton         imgFolderSelBtn = new JButton( "Select image folder" ) ;
    private JButton         moveImgsBtn     = new JButton( ">" ) ;
    private JButton         removeChapterBtn= new JButton( "X" ) ;
    private JFileChooser    fileChooser     = new JFileChooser() ;
    
    private DefaultListModel<String> listModel = new DefaultListModel<>() ;
    private JList<String>            imageList = new JList<>( listModel ) ;
    
    private Map<String, File> fileMap = new HashMap<>() ;
    
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
        
        JScrollPane sp = new JScrollPane( imageList ) ;
        add( sp, BorderLayout.CENTER ) ;
        add( btnPanel, BorderLayout.NORTH ) ;
        add( moveImgsBtn, BorderLayout.EAST ) ;
        
        setUpEventListeners() ;
    }
    
    private void setUpEventListeners() {
        imgFolderSelBtn.addActionListener( this ) ;
        removeChapterBtn.addActionListener( this ) ;
        moveImgsBtn.addActionListener( this ) ;
        imageList.addListSelectionListener( this ) ;
        imageList.addKeyListener(new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
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
    
    private void imageFolderChanged( File newFolder ) {
        
        this.imgFolder = newFolder ;
        imageList.removeAll() ;
        
        File[] files = this.imgFolder.listFiles( new FileFilter() {
            public boolean accept( File file ) {
                if( file.getName().endsWith( ".png" ) && 
                    !file.getName().startsWith( "Page" ) ) {
                    return true ;
                }
                return false ;
            }
        } ) ;
        
        if( files != null && files.length > 0 ) {
            DefaultListModel<String> model = ( DefaultListModel<String> )imageList.getModel() ;
            Arrays.sort( files, new Comparator<File>() {
                public int compare( File f1, File f2 ) {
                    return (int)(f1.lastModified() - f2.lastModified()) ;
                }
            } ) ;
            for( File file : files ) {
                validateAndCatalogFile( file ) ;
                model.addElement( file.getName() ) ;
            }
        }
    }
    
    private void validateAndCatalogFile( File file ) {
        
        String fName = file.getName() ;
        String marker = fName.substring( 0, fName.indexOf( "_", fName.indexOf( "_" ) + 1 ) ) ;
        
        File smallImgFile = new File( this.imgFolder, "Page_" + marker + ".png" ) ;
        if( smallImgFile.exists() ) {
            fileMap.put( fName, smallImgFile ) ;
        }
        else {
            throw new RuntimeException( "File " + fName + " doesn't have an associated small file." ) ;
        }
    }
    
    private void moveSelectedImages() {
        
        List<String> selectedFiles = imageList.getSelectedValuesList() ;
        List<File> largeFiles = new ArrayList<>() ;
        List<File> smallFiles = new ArrayList<>() ;
        
        for( String name : selectedFiles ) {
            largeFiles.add( new File( this.imgFolder, name ) ) ;
            smallFiles.add( this.fileMap.get( name ) ) ;
        }
        
        String moveResult = this.parent.moveFiles( largeFiles, smallFiles ) ;
        if( moveResult == null ) {
            for( String name : selectedFiles ) {
                listModel.removeElement( name ) ;
                fileMap.remove( name ) ;
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
            imgFile = fileMap.get( files.get( files.size()-1 ) ) ;
        }
        
        this.parent.showThumbnail( imgFile ) ;
    }
}
