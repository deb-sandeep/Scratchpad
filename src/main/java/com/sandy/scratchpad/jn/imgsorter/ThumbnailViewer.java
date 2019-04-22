package com.sandy.scratchpad.jn.imgsorter;

import java.awt.Dimension ;
import java.awt.Image ;
import java.awt.image.BufferedImage ;
import java.io.File ;
import java.io.IOException ;

import javax.imageio.ImageIO ;
import javax.swing.ImageIcon ;
import javax.swing.JLabel ;

@SuppressWarnings( "serial" )
public class ThumbnailViewer extends JLabel {

    public ThumbnailViewer() {
        setPreferredSize( new Dimension( 400, 1 ) ) ;
    }
    
    public void showImage( File imgFile ) {
        
        if( imgFile == null || !imgFile.exists() ) {
            setIcon( null ) ;
            return ;
        }
        
        BufferedImage bufferedImg = null ;
        Image scaledImg = null ;
        
        try {
            bufferedImg = ImageIO.read( imgFile ) ;
            scaledImg = bufferedImg.getScaledInstance( getWidth(), -1,
                                                       Image.SCALE_SMOOTH ) ;
            
            setIcon( new ImageIcon( scaledImg ) ) ;
        } 
        catch( IOException e ) {
            e.printStackTrace() ;
        }
    }
    
}
