package com.sandy.scratchpad.dotgrid;

import java.awt.Color ;
import java.awt.Graphics2D ;
import java.awt.image.BufferedImage ;
import java.io.File ;

import javax.imageio.ImageIO ;

public class DotGridGen {
    
    public void generateDotGrid( int width, int height, int cellWidth ) 
        throws Exception {
        
        BufferedImage bufferedImage = null ;
        Graphics2D g2d = null ;
        
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        g2d = bufferedImage.createGraphics();
        drawGrid( width, height, cellWidth, g2d ) ;
        g2d.dispose();
 
        writeImage( bufferedImage ) ;
    }
    
    private void drawGrid( int width, int height, int cellWidth, Graphics2D g2d ) {
        
        g2d.setColor( Color.BLACK ) ;
        g2d.fillRect( 0, 0, width, height ) ;

        g2d.setColor( Color.DARK_GRAY.darker() ) ;
        for( int pixX=0; pixX < width; pixX+=cellWidth ) {
            for( int pixY=0; pixY < height; pixY+=cellWidth ) {
                g2d.drawOval( pixX, pixY, 1, 1 ) ;
            }
        }
    }
    
    private void writeImage( BufferedImage image ) 
        throws Exception {
        
        File file = new File( "c:\\temp\\dotgrid.png" ) ;
        ImageIO.write( image, "png", file ) ;
        System.out.println( "File generated." ) ;
    }

    public static void main(String[] args) throws Exception {
        
        int width = 1200 ;
        int height = 631 ;
        int cellWidth = 20 ;
 
        new DotGridGen().generateDotGrid( width, height, cellWidth ) ;
    }
}
