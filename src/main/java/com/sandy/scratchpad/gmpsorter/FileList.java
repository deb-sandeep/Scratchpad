package com.sandy.scratchpad.gmpsorter;

import java.io.File ;
import java.io.FileFilter ;
import java.util.Arrays ;
import java.util.Comparator ;

import javax.swing.DefaultListModel ;
import javax.swing.JList ;

import org.apache.log4j.Logger ;

class Question {
    
    String paperId = null ;
    String qType = null ;
    float qId = 0.0F ;
    
// AITS
//    Question( String fileName ) {
//        String[] parts = fileName.substring( 0, fileName.length()-4 ).split( "_" ) ;
//        paperId = parts[2] + "_" + parts[3] ;
//        qType = parts[4] ;
//        String qIdStr = "" ;
//        
//        int qIdPartsLen = parts.length - 5 ;
//        for( int i=0; i<qIdPartsLen; i++ ) {
//            qIdStr += parts[i+5] ;
//            qIdStr += "." ;
//        }
//        qIdStr = qIdStr.substring( 0, qIdStr.length()-1 ) ;
//        if( qIdStr.contains( "(" ) ) {
//            qIdStr = qIdStr.substring( 0, qIdStr.indexOf( '(' ) ) ;
//        }
//        qId = Float.parseFloat( qIdStr ) ;
//    }
//

//    YG & Allen
    Question( String fileName ) {
        System.out.println( fileName ) ;
        String[] parts = fileName.substring( 0, fileName.length()-4 ).split( "_" ) ;
        paperId = parts[2] ;
        qType = parts[3] ;
        String qIdStr = "" ;
        
        int qIdPartsLen = parts.length - 4 ;
        for( int i=0; i<qIdPartsLen; i++ ) {
            qIdStr += parts[i+4] ;
            qIdStr += "." ;
        }
        qIdStr = qIdStr.substring( 0, qIdStr.length()-1 ) ;
        if( qIdStr.contains( "(" ) ) {
            qIdStr = qIdStr.substring( 0, qIdStr.indexOf( '(' ) ) ;
        }
        qId = Float.parseFloat( qIdStr ) ;
    }
}

@SuppressWarnings( "serial" )
public class FileList extends JList<String> {
    
    static final Logger log = Logger.getLogger( FileList.class ) ;

    private File baseDir = null ;
    private DefaultListModel<String> listModel = new DefaultListModel<>() ;
    
    public FileList( GMPSorter sorter ) {
        this.baseDir = sorter.getBaseDir() ;
        populateModel() ;
        setModel( listModel ) ;
        addListSelectionListener( sorter ) ;
    }
    
    public int getNumFiles() {
        return listModel.getSize() ;
    }
    
    private void populateModel() {
        File[] files = this.baseDir.listFiles( new FileFilter() {
            public boolean accept( File file ) {
                String fileName = file.getName() ;
                return fileName.contains( GMPSorter.IMG_MATCH ) ;
            }
        } ) ;
        
        sortFileArray( files ) ;
        
        for( File file : files ) {
            listModel.addElement( file.getName() ) ;
        }
    }

    private void sortFileArray( File[] files ) {
        
        Arrays.sort( files, new Comparator<File>() {
            public int compare( File f1, File f2 ) {
                Question q1 = new Question( f1.getName() ) ;
                Question q2 = new Question( f2.getName() ) ;
                
                if( q1.paperId.equals( q2.paperId ) ) {
                    if( q1.qType.equals( q2.qType ) ) {
                        return ( q1.qId < q2.qId ) ? -1 : 1 ;
                    }
                    return q1.qType.compareTo( q2.qType ) ;
                }
                return q1.paperId.compareTo( q2.paperId ) ;
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
