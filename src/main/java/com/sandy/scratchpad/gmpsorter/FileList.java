package com.sandy.scratchpad.gmpsorter;

import java.io.File ;
import java.io.FileFilter ;
import java.util.Arrays ;
import java.util.Comparator ;

import javax.swing.DefaultListModel ;
import javax.swing.JList ;

@SuppressWarnings( "serial" )
public class FileList extends JList<String> {

    private File baseDir = null ;
    private DefaultListModel<String> listModel = new DefaultListModel<>() ;
    
    public FileList( GMPSorter sorter ) {
        this.baseDir = sorter.getBaseDir() ;
        populateModel() ;
        setModel( listModel ) ;
        addListSelectionListener( sorter ) ;
    }
    
    private void populateModel() {
        File[] files = this.baseDir.listFiles( new FileFilter() {
            public boolean accept( File file ) {
                return file.getName().startsWith( "Math_Q_GMP_" ) ;
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
                
                String type1 = getQType( f1.getName() ) ;
                String type2 = getQType( f2.getName() ) ;
                
                if( !type1.equals( type2 ) ) {
                    return type1.compareTo( type2 ) ;
                }
                
                int f1IntId = getIntegerId( f1.getName() ) ;
                int f2IntId = getIntegerId( f2.getName() ) ;
                
                int f1IntPart = (int)Math.floor( f1IntId ) ;
                int f2IntPart = (int)Math.floor( f2IntId ) ;
                
                if( f1IntPart < f2IntPart ) {
                    return -1 ;
                }
                else if( f1IntPart > f2IntPart ) {
                    return 1 ;
                }
                
                int f1DecimalId = getDecimalId( f1.getName() ) ;
                int f2DecimalId = getDecimalId( f2.getName() ) ;
                
                if( f1DecimalId < f2DecimalId ) {
                    return -1 ;
                }
                else if( f1DecimalId > f2DecimalId ) {
                    return 1 ;
                }

                return 0 ;
            }
        } ) ;
    }
    
    private String getQType( String fileName ) {
        String typePart = fileName.substring( "Chem_Q_GMP_".length() ) ;
        if( typePart.contains( "_" ) ) {
            return typePart.substring( 0, typePart.indexOf( '_' ) ) ;
        }
        return "" ;
    }
    
    private int getIntegerId( String fileName ) {
        String intStr = fileName.substring( fileName.lastIndexOf( "_" ) + 1 ) ;
        intStr = intStr.substring( 0, intStr.indexOf( '.' )  ) ;
        return Integer.parseInt( intStr ) ;
    }
    
    private int getDecimalId( String fileName ) {
        String intStr = fileName.substring( fileName.lastIndexOf( "_" ) + 1 ) ;
        intStr = intStr.substring( intStr.indexOf( '.' )+1  ) ;
        if( intStr.contains( "." ) ) {
            intStr = intStr.substring( 0, intStr.indexOf( "." ) ) ;
            return Integer.parseInt( intStr ) ;
        }
        return 0 ;
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
