package com.sandy.scratchpad.jee.imgsorter;

import org.apache.log4j.Logger ;

import javax.swing.*;
import java.awt.*;
import java.io.File ;

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

        label.setText( ( (File)value ).getName() ) ;
        return this ;
    }
}
