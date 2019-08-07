package com.sandy.scratchpad.gmpsorter;

import java.io.File ;
import java.io.FileFilter ;
import java.util.Arrays ;
import java.util.Comparator ;

import javax.swing.DefaultListModel ;
import javax.swing.JList ;

import org.apache.log4j.Logger ;

@SuppressWarnings( "serial" )
public class TopicList extends JList<String> {

    static Logger log = Logger.getLogger( TopicList.class ) ;
    
    private File baseDir = null ;
    private DefaultListModel<String> listModel = new DefaultListModel<>() ;

    public TopicList( GMPSorter sorter ) {
        this.baseDir = sorter.getBaseDir() ;
        populateModel() ;
        setModel( listModel ) ;
        setSelectedIndex( 0 ) ;
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
        
        for( File dir : topicDirs ) {
            listModel.addElement( dir.getName() ) ;
        }
    }
}
