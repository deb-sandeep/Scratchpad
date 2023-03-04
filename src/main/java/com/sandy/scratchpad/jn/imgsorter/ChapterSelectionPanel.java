package com.sandy.scratchpad.jn.imgsorter;

import java.awt.BorderLayout ;
import java.awt.event.ActionEvent ;
import java.awt.event.ActionListener ;
import java.io.File ;
import java.io.FileFilter ;
import java.io.IOException ;
import java.util.Arrays ;
import java.util.Comparator ;
import java.util.List ;

import javax.swing.DefaultListModel ;
import javax.swing.JButton ;
import javax.swing.JFileChooser ;
import javax.swing.JList ;
import javax.swing.JPanel ;
import javax.swing.JScrollPane ;
import javax.swing.ListSelectionModel ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;
import com.sandy.scratchpad.jn.chpage.ChPageCreator ;

@SuppressWarnings( "serial" )
public class ChapterSelectionPanel extends JPanel 
    implements ActionListener {

    static final Logger log = Logger.getLogger( ImageSelectionPanel.class ) ;
    
    private JNImageSorter parent = null ;
    private JButton       subFolderSelBtn = new JButton( "Select subject folder" ) ;
    private JButton       removeChapterBtn= new JButton( "X" ) ;
    private JFileChooser  fileChooser     = new JFileChooser() ;
    
    private DefaultListModel<String> listModel   = new DefaultListModel<>() ;
    private JList<String>            chapterList = new JList<>( listModel ) ;
    
    private File subjectFolder = null ;
    
    public ChapterSelectionPanel( JNImageSorter parent ) {
        this.parent = parent ;
        
        File subDir = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes-Std-9/Class-9" ) ;
        if( subDir.exists() ) {
            subjectFolder = subDir ;
        }

        setUpUI() ;
    }

    private void setUpUI() {
        setLayout( new BorderLayout() ) ;
        
        if( subjectFolder != null ) {
            fileChooser.setCurrentDirectory( subjectFolder ) ;
        }
        fileChooser.setFileSelectionMode( JFileChooser.DIRECTORIES_ONLY ) ;
        fileChooser.setDialogTitle( "Select image folder" ) ;
        fileChooser.setMultiSelectionEnabled( false ) ;
        
        chapterList.setSelectionMode( ListSelectionModel.SINGLE_SELECTION ) ;
        
        JScrollPane sp = new JScrollPane( chapterList ) ;
        JPanel btnPanel = new JPanel( new BorderLayout() ) ;
        btnPanel.add( subFolderSelBtn, BorderLayout.CENTER ) ;
        btnPanel.add( removeChapterBtn, BorderLayout.EAST ) ;
        
        add( btnPanel, BorderLayout.NORTH ) ;
        add( sp, BorderLayout.CENTER ) ;
        
        setUpEventListeners() ;
    }
    
    private void setUpEventListeners() {
        subFolderSelBtn.addActionListener( this ) ;
        removeChapterBtn.addActionListener( this ) ;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        
        if( e.getSource() == subFolderSelBtn ) {
            if( fileChooser.showOpenDialog( this.parent ) == JFileChooser.APPROVE_OPTION ) {
                subjectFolderChanged( fileChooser.getSelectedFile() ) ;
            } ;
        }
        else if( e.getSource() == removeChapterBtn ) {
            if( chapterList.getSelectedIndex() != -1 ) {
                listModel.remove( chapterList.getSelectedIndex() ) ;
                if( !listModel.isEmpty() ) {
                    chapterList.setSelectedIndex( 0 ) ;
                }
            }
        }
    }
    
    private void subjectFolderChanged( File newFolder ) {
        
        this.subjectFolder = newFolder ;
        chapterList.removeAll() ;
        
        File[] files = this.subjectFolder.listFiles( new FileFilter() {
            public boolean accept( File file ) {
                if( file.isDirectory() && 
                    file.getName().matches( "^[0-9]+ - .*$" ) ) {
                    return true ;
                }
                return false ;
            }
        } ) ;
        
        if( files != null && files.length > 0 ) {
            DefaultListModel<String> model = ( DefaultListModel<String> )chapterList.getModel() ;
            Arrays.sort( files, new Comparator<File>() {
                public int compare( File f1, File f2 ) {
                    return f1.getName().compareTo( f2.getName() ) ;
                }
            } ) ;
            
            for( File file : files ) {
                model.addElement( file.getName() ) ;
            }
            chapterList.setSelectedIndex( 0 ) ;
        }
    }
    
    public String acceptFiles( List<File> largeFiles,
                               boolean removeTargetFolderAfterMove ) 
        throws Exception {
        
        String message = null ;
        String selectedChapter = chapterList.getSelectedValue() ;
        
        if( selectedChapter != null ) {
            
            File chapterFolder = new File( this.subjectFolder, selectedChapter ) ;
            String pagesFolderPath = "img/pages" ;
            
            if( StringUtil.isNotEmptyOrNull( JNImageSorter.BOOK_NAME ) ) {
                pagesFolderPath = "img/" + JNImageSorter.BOOK_NAME + "/pages" ;
            }
            
            File pageFolder  = new File( chapterFolder, pagesFolderPath ) ;
            
            moveFiles( largeFiles, pageFolder ) ;
            
            if( JNImageSorter.CREATE_JN_FILE ) {
                createJNFileForPages( selectedChapter ) ;
            }
            
            if( removeTargetFolderAfterMove ) {
                listModel.removeElement( selectedChapter ) ;
                if( !listModel.isEmpty() ) {
                    chapterList.setSelectedIndex( 0 ) ;
                }
            }
        }
        else {
            message = "Please select a target chapter." ;
        }
        return message ;
    }
    
    private void moveFiles( List<File> files, File destFolder ) 
        throws IOException {
        
        for( File srcFile : files ) {
            
            String destFileName = "Page_" + getNextFileNumber( destFolder ) + ".png";
            File destFile = new File( destFolder, destFileName ) ;
            
            //FileUtils.moveFile( srcFile, destFile ) ;
            FileUtils.copyFile( srcFile, destFile ) ;
            
            File dummyFile = new File( destFolder, "dummy.txt" ) ;
            if( dummyFile.exists() ) {
                dummyFile.delete() ;
            }
        }
    }
    
    private int getNextFileNumber( File destFolder ) {
        
        File[] files = destFolder.listFiles( new FileFilter() {
            @Override
            public boolean accept( File file ) {
                return !file.isDirectory() && file.getName().endsWith( ".png" ) ;
            }
        } ) ;
        
        return files == null ? 1 : files.length+1 ;
    }
    
    private void createJNFileForPages( String selectedChapter ) 
        throws Exception {
        
        ChPageCreator pageCreator = new ChPageCreator() ;
        File chapterFolder = new File( this.subjectFolder, selectedChapter ) ;
        String subjectName = this.subjectFolder.getName() ;
        
        pageCreator.createPageForChapter( subjectName, chapterFolder ) ;
    }
}
