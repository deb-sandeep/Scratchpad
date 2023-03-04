package com.sandy.scratchpad.jn.imgsorter;

import java.awt.Component ;
import java.io.File ;
import java.util.ArrayList ;
import java.util.List ;

import javax.swing.DefaultListCellRenderer ;
import javax.swing.JLabel ;
import javax.swing.JList ;

import org.apache.log4j.Logger ;

public class ImageListRenderer extends DefaultListCellRenderer {

    private static final long serialVersionUID = 1L ;
    static final Logger log = Logger.getLogger( ImageListRenderer.class ) ;

    
    @Override
    public Component getListCellRendererComponent( JList<?> list,
                                                   Object value,
                                                   int index,
                                                   boolean isSelected,
                                                   boolean cellHasFocus) {
        
        JLabel label = ( JLabel )super.getListCellRendererComponent( 
                                list, value, index, isSelected, cellHasFocus ) ;

        List<String> displayParts = getDisplayParts( (File)value ) ;
        StringBuilder sb = new StringBuilder() ;
        
        for( String part : displayParts ) {
            sb.append( part ).append( "/" ) ;
        }
        sb.deleteCharAt( sb.length()-1 ) ;
        
        label.setText( sb.toString() ) ;
        return this ;
    }
    
    private List<String> getDisplayParts( File file ) {
        
        String absolutePath = file.getAbsolutePath() ;
        String[] parts = absolutePath.split( File.separator ) ;
        
        boolean bookNameEncountered = false ;
        List<String> displayParts = new ArrayList<>() ;
        
        displayParts.add( parts[parts.length-1] ) ;
        for( int i=parts.length-2; i>=0; i-- ) {
            String part = parts[i] ;
            if( part.equals( "pages" ) || 
                part.equals( "img" ) ) {
                
                continue ;
            }
            
            if( part.equals( "books" ) ) {
                bookNameEncountered = true ;
                continue ;
            }
            
            displayParts.add( 0, part ) ;
            
            if( bookNameEncountered  && displayParts.size() == 3 ) break ;
            if( !bookNameEncountered && displayParts.size() == 2 ) break ;
        }
        
        return displayParts ;
    }
}
