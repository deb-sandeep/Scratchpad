package com.sandy.scratchpad.jeeqsorter ;

import java.awt.BorderLayout ;
import java.awt.Container ;
import java.awt.Dimension ;
import java.awt.Font ;
import java.awt.KeyEventDispatcher ;
import java.awt.KeyboardFocusManager ;
import java.awt.event.KeyEvent ;
import java.io.File ;
import java.io.FileWriter ;
import java.io.IOException ;
import java.util.Stack ;

import javax.swing.JComponent ;
import javax.swing.JFrame ;
import javax.swing.JList ;
import javax.swing.JPanel ;
import javax.swing.JScrollPane ;
import javax.swing.event.ListSelectionEvent ;
import javax.swing.event.ListSelectionListener ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.scratchpad.jn.imgsorter.ThumbnailViewer ;

@SuppressWarnings( "serial" )
public class JEEQSorter extends JFrame implements ListSelectionListener {
    
    static final Logger log = Logger.getLogger( JEEQSorter.class ) ;
    
    public static final String BK_ALLEN = "Allen" ;
    public static final String BK_AITS  = "AITS" ;
    public static final String BK_YG202 = "YG File 2 - 2020" ;
    
    public static final String SUB_MATHS = "IIT - Maths" ;
    public static final String SUB_PHY   = "IIT - Physics" ;
    public static final String SUB_CHEM  = "IIT - Chemistry" ;
    
    public static final String IMG_MATCH = "_Q_" ;
    
    static final Font LIST_FONT = new Font( "Arial", Font.PLAIN, 14 ) ;
    
    private static final int WIDTH = 900 ;
    
    private FileList fileList = null ;
    private TopicButtonPanel topicBtnPanel = null ;
    private ThumbnailViewer imgViewer = null ;
    
    private File baseDestDir = null ;
    private File baseSrcDir = null ;
    private File qmetaCSVFile = null ;
    private File classificationLog = null ;
    
    private Stack<File> undoStack = new Stack<>() ;

    public JEEQSorter( File baseDestDir, File baseSrcDir ) {
        super( "GMP question sorter." ) ;
        this.baseDestDir = baseDestDir ;
        this.baseSrcDir = baseSrcDir ;
        this.qmetaCSVFile = new File( baseDestDir, "meta.csv" ) ;
        this.classificationLog = new File( baseDestDir, "classification-log.csv" ) ;
        
        setUpUI() ;
        setUpListeners() ;
        
        setVisible( true ) ;
        
        imgViewer.requestFocus() ;
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
        panel.add( getImageViewer(), BorderLayout.NORTH ) ;
        panel.add( getListPanel(), BorderLayout.CENTER ) ;
        return panel ;
    }
    
    private JPanel getListPanel() {

        this.fileList = new FileList( this ) ;
        this.fileList.setFont( LIST_FONT );
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
                File imgFile = new File( getBaseSrcDir(), list.getSelectedValue() ) ;
                this.imgViewer.showImage( imgFile ) ;
                
                String srcFName = imgFile.getName() ;
                if( srcFName.startsWith( "Phy_Q_" ) ) {
                    topicBtnPanel.showTab( SUB_PHY ) ;
                }
                else if( srcFName.startsWith( "Chem_Q_" ) ) {
                    topicBtnPanel.showTab( SUB_CHEM ) ;
                }
                else if( srcFName.startsWith( "Math_Q_" ) ) {
                    topicBtnPanel.showTab( SUB_MATHS ) ;
                }

            }
        }
    }
    
    public File getBaseDestDir() {
        return this.baseDestDir ;
    }
    
    public File getBaseSrcDir() {
        return this.baseSrcDir ;
    }
    
    private void processKeyStroke( char key ) {
        switch( key ) {
            case 'z':
                undo() ;
                break ;
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
        File srcFile = new File( getBaseSrcDir(), fileList.getSelectedValue() ) ;
        File destDir = getDestFolder( srcFile ) ;
        File destFile = new File( destDir, fileList.getSelectedValue() ) ;
        
        try {
            FileUtils.moveFile( srcFile, destFile ) ;
            
            fileList.removeSelectedValue() ;
            undoStack.push( destFile ) ;
            setTitle( "Num files left = " + fileList.getNumFiles() ) ;
            makeMetaEntry( srcFile ) ;
            makeClassificationLogEntry( srcFile, destFile ) ;
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }
    
    private void makeMetaEntry( File srcFile ) {
        
        Question question = new Question( srcFile.getName() ) ;

        if( !question.isLCTParagraph() ) {
            StringBuilder sb = new StringBuilder() ;
            sb.append( question.sub )
              .append( "," )
              .append( question.paperId )
              .append( "," )
              .append( question.qType )
              .append( "," )
              .append( question.qNumber ) ;
            try {
                FileWriter fw = new FileWriter( qmetaCSVFile, true ) ;
                fw.append( sb.toString() + "\n" ) ;
                fw.flush() ;
                fw.close() ;
            }
            catch( IOException e ) {
                log.error( "Could not save meta" ) ;
            }
        }
    }
    
    private void makeClassificationLogEntry( File src, File dest ) {
        
        String srcFName = src.getName() ;
        String entry = srcFName.substring( 0, srcFName.length()-4 ) + "," + 
                       dest.getParent()
                           .substring( baseDestDir.getAbsolutePath()
                                                  .length() ) ;
        
        try {
            FileWriter fw = new FileWriter( classificationLog, true ) ;
            fw.append( entry + "\n" ) ;
            fw.flush() ;
            fw.close() ;
            log.debug( entry ) ;
        }
        catch( IOException e ) {
            log.error( "Could not save classification" ) ;
        }
    }
    
    private File getDestFolder( File srcFile ) {
        String srcFName = srcFile.getName() ;
        String bookFolderName = null ;
        String subFolderName = null ;
        
        if( srcFName.contains( "_Q_FJ" ) ) {
            bookFolderName = BK_AITS ;
        }
        else if( srcFName.contains( "_Q_AL" ) ) {
            bookFolderName = BK_ALLEN ;
        }
        if( srcFName.contains( "_Q_YG202" ) ) {
            bookFolderName = BK_YG202 ;
        }
        
        if( srcFName.startsWith( "Phy_Q_" ) ) {
            subFolderName = SUB_PHY ;
        }
        else if( srcFName.startsWith( "Chem_Q_" ) ) {
            subFolderName = SUB_CHEM ;
        }
        else if( srcFName.startsWith( "Math_Q_" ) ) {
            subFolderName = SUB_MATHS ;
        }
        
        File destFolder = new File( this.baseDestDir, subFolderName ) ;
        destFolder = new File( destFolder, topicBtnPanel.getSelectedDirName() ) ;
        destFolder = new File( destFolder, bookFolderName ) ;
        
        return destFolder ;
    }
    
    private void undo() {
        
        if( !undoStack.isEmpty() ) {
            File lastMovedFile = undoStack.pop() ;
            File destFile = new File( getBaseSrcDir(), lastMovedFile.getName() ) ;
            
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
        File baseDestDir = new File( "/home/sandeep/projects/source/SConsoleProcessedImages/" ) ;
        File baseSrcDir = new File( "/home/sandeep/temp/question-scrapes" ) ;
        new JEEQSorter( baseDestDir, baseSrcDir ) ;
    }

}
