package com.sandy.scratchpad.dotgrid;

import java.awt.Color ;
import java.awt.Graphics2D ;
import java.awt.image.BufferedImage ;
import java.io.File ;

import javax.imageio.ImageIO ;

public class DotGridGen {
    
    public void generateDotGrid( int width, int height, int cellWidth ) 
        throws Exception {
        
        BufferedImage bufferedImage ;
        Graphics2D g2d ;
        
        bufferedImage = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        g2d = bufferedImage.createGraphics();
        drawGrid( width, height, cellWidth, g2d ) ;
        g2d.dispose();
 
        writeImage( bufferedImage ) ;
    }
    
    private void drawGrid( int width, int height, int cellWidth, Graphics2D g2d ) {
        
        g2d.setColor( new Color( 30, 31, 34 ) ) ;
        g2d.fillRect( 0, 0, width, height ) ;

        g2d.setColor( Color.LIGHT_GRAY ) ;
        int numRows = 0 ;
        for( int pixX=0; pixX < width; pixX+=cellWidth ) {
            for( int pixY=0; pixY < height; pixY+=cellWidth ) {
                g2d.drawOval( pixX, pixY, 2, 2 ) ;
            }
            numRows++ ;
        }
        System.out.println( numRows ) ;
    }
    
    private void writeImage( BufferedImage image ) 
        throws Exception {
        
        File file = new File( "/Users/sandeep/temp/dotgrid.png" ) ;
        ImageIO.write( image, "png", file ) ;
        System.out.println( "File generated." ) ;
    }

    public static void main(String[] args) throws Exception {
        
        int width = 2486 ;
        int height = 3480 ;
        int cellWidth = 100 ;
 
        new DotGridGen().generateDotGrid( width, height, cellWidth ) ;
    }
}
