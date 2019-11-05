package com.sandy.scratchpad.gmpsorter;

import java.awt.Font ;
import java.awt.GridLayout ;
import java.awt.event.ActionEvent ;
import java.awt.event.ActionListener ;
import java.io.File ;
import java.io.FileFilter ;
import java.util.ArrayList ;
import java.util.Arrays ;
import java.util.Comparator ;
import java.util.List ;

import javax.swing.JButton ;
import javax.swing.JPanel ;

import org.apache.log4j.Logger ;

@SuppressWarnings( "serial" )
public class TopicButtonPanel extends JPanel implements ActionListener {

    static Logger log = Logger.getLogger( TopicButtonPanel.class ) ;
    
    static final Font BTN_FONT = new Font( "Arial", Font.PLAIN, 18 ) ;
    
    private File baseDir = null ;
    private List<String> topicNames = new ArrayList<>() ;
    private GMPSorter sorter = null ;
    private String selectedTopic = null ;

    public TopicButtonPanel( GMPSorter sorter ) {
        this.sorter = sorter ;
        this.baseDir = sorter.getBaseDir() ;
        populateModel() ;
        setUpUI() ;
    }
    
    private void populateModel() {
        File[] topicDirs = this.baseDir.listFiles( new FileFilter() {
            public boolean accept( File pathname ) {
                return pathname.isDirectory() ;
            }
        } ) ;
        
        Arrays.sort( topicDirs, new Comparator<File>() {
            public int compare( File f1, File f2 ) {
                return f1.getName().compareTo( f2.getName() ) ;
            }
        } ) ;
        
        for( int i=0; i<topicDirs.length; i++ ) {
            File dir = topicDirs[i] ;
            String topicName = dir.getName() ;
            topicNames.add( topicName ) ;
        }
    }
    
    private void setUpUI() {
        
        int numColumns = 6 ;
        int numRows = 4 ;
        
        setLayout( new GridLayout( numRows, numColumns ) ) ;
        for( String topic : topicNames ) {
            JButton btn = new JButton( getWrappedText( topic ) ) ;
            btn.addActionListener( this ) ;
            btn.setFont( BTN_FONT ) ;
            btn.setActionCommand( topic ) ;
            add( btn ) ;
        }
        
        JButton btn = new JButton( " >> " ) ;
        btn.addActionListener( this ) ;
        btn.setFont( BTN_FONT ) ;
        add( btn ) ;
    }
    
    public String getSelectedDirName() {
        return this.selectedTopic ;
    }
    
    private String getWrappedText( String text ) {
        
        if( text.length() > 15 ) {
            StringBuffer buffer = new StringBuffer() ;
            buffer.append( "<html><body>" ) ;
            buffer.append( text ) ;
            buffer.append( "</body></html>" ) ;
            return buffer.toString() ;
        }
        return text ;
    }

    @Override
    public void actionPerformed( ActionEvent e ) {
        JButton src = ( JButton )e.getSource() ;
        this.selectedTopic = src.getText() ;
        if( this.selectedTopic.equals( " >> " ) ) {
            this.sorter.nextImage() ;
        }
        else {
            this.selectedTopic = src.getActionCommand() ;
            this.sorter.moveImageFile() ;
        }
    }
}
