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
        
        for( int i=0; i<topicDirs.length; i++ ) {
            File dir = topicDirs[i] ;
            String listVal = String.format( "%02d - %s", (i+1), dir.getName() ) ;
            listModel.addElement( listVal ) ;
        }
    }
    
    public String getSelectedDirName() {
        String selVal = getSelectedValue() ;
        return selVal.substring( 5 ) ;
    }

    public void selectValueWithFirstLetter( char key ) {
        int selIndex = getSelectedIndex() ;
        int upDistance = 999 ;
        int downDistance = 999 ;
        
        for( int i=selIndex-1; i>=0; i-- ) {
            if( listModel.get( i ).charAt( 5 ) == Character.toUpperCase( key ) ) {
                upDistance = selIndex-i ;
                break ;
            }
        }

        for( int i=selIndex+1; i<listModel.size(); i++ ) {
            if( listModel.get( i ).charAt( 5 ) == Character.toUpperCase( key ) ) {
                downDistance = i-selIndex ;
                break ;
            }
        }
        
        int newSelIndex = selIndex ;
        if( upDistance < downDistance ) {
            newSelIndex = selIndex-upDistance ;
        }
        else {
            newSelIndex = selIndex+downDistance;
        }
        
        if( newSelIndex != selIndex ) {
            setSelectedIndex( newSelIndex ) ;
        }
    }
}
