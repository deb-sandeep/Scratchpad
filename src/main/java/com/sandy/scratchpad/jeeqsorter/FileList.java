package com.sandy.scratchpad.jeeqsorter;

import java.io.File ;
import java.io.FileFilter ;
import java.util.ArrayList ;
import java.util.Arrays ;
import java.util.Comparator ;
import java.util.List ;

import javax.swing.DefaultListModel ;
import javax.swing.JList ;
import javax.swing.SwingUtilities ;

import org.apache.log4j.Logger ;

@SuppressWarnings( "serial" )
public class FileList extends JList<String> {
    
    static final Logger log = Logger.getLogger( FileList.class ) ;

    private File baseDir = null ;
    private DefaultListModel<String> listModel = new DefaultListModel<>() ;
    
    private Thread filePoller = new Thread( new Runnable() {
        public void run() {
            while( true ) {
                try {
                    Thread.sleep( 10000 ) ;
                    populateModel() ;
                }
                catch( Exception e ) {
                    e.printStackTrace() ; 
                }
            }
        }
    } ) ;
    
    public FileList( JEEQSorter sorter ) {
        this.baseDir = sorter.getBaseSrcDir() ;
        populateModel() ;
        setModel( listModel ) ;
        addListSelectionListener( sorter ) ;
        
        filePoller.setDaemon( true ) ;
        filePoller.start() ;
    }
    
    public int getNumFiles() {
        return listModel.getSize() ;
    }
    
    private void populateModel() {
        
        log.debug( "Scanning..." ) ;

        List<String> elementsToRemove = new ArrayList<>() ;
        for( int i=0; i<listModel.size(); i++ ) {
            String entry = listModel.get( i ) ;
            File file = new File( this.baseDir, entry ) ;
            if( !file.exists() ) {
                elementsToRemove.add( entry ) ;
            }
        }

        if( !elementsToRemove.isEmpty() ) {
            SwingUtilities.invokeLater( new Runnable() {
                public void run() {
                    for( String entry : elementsToRemove ) {
                        listModel.removeElement( entry ) ;
                        log.debug( "Removed -> " + entry ) ;
                    }
                }
            } ) ;
        }
        
        File[] files = this.baseDir.listFiles( new FileFilter() {
            public boolean accept( File file ) {
                String fileName = file.getName() ;
                return fileName.contains( JEEQSorter.IMG_MATCH ) ;
            }
        } ) ;
        
        sortFileArray( files ) ;
        
        for( File file : files ) {
            if( !listModel.contains( file.getName() ) ) {
                SwingUtilities.invokeLater( new Runnable() {
                    public void run() {
                        log.debug( "Discovered -> " + file.getName() ) ;
                        listModel.addElement( file.getName() ) ;
                    }
                } ) ;
            }
        }
    }

//    private void sortFileArray( File[] files ) {
//        
//        Arrays.sort( files, new Comparator<File>() {
//            public int compare( File f1, File f2 ) {
//                Question q1 = new Question( f1.getName() ) ;
//                Question q2 = new Question( f2.getName() ) ;
//                
//                if( q1.sub.equals( q2.sub ) ) {
//                    if( q1.paperId.equals( q2.paperId ) ) {
//                        if( q1.qType.equals( q2.qType ) ) {
//                            return ( q1.qId < q2.qId ) ? -1 : 1 ;
//                        }
//                        return q1.qType.compareTo( q2.qType ) ;
//                    }
//                    return q1.paperId.compareTo( q2.paperId ) ;
//                }
//                return q1.sub.compareTo( q2.sub ) ;
//            }
//        } ) ;
//    }
//
    private void sortFileArray( File[] files ) {
        
        Arrays.sort( files, new Comparator<File>() {
            public int compare( File f1, File f2 ) {
                return (int)(f1.lastModified() - f2.lastModified()) ;
            }
        } ) ;
    }

    public void removeSelectedValue() {
        int index = getSelectedIndex() ;
        if( index != -1 ){
            listModel.remove( index ) ;
            if( listModel.size() > index ) {
                setSelectedIndex( index ) ;
            }
            else if( listModel.size() > 0 ) {
                setSelectedIndex( index-1 ) ;
            }
        }
    }

    public void insert( String name ) {
        int selectedIndex = getSelectedIndex() ;
        listModel.add( getSelectedIndex(), name ) ;
        setSelectedIndex( selectedIndex ) ;
    }
}
