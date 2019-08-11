package com.sandy.scratchpad.jn.gmpimgrefactor;

import java.awt.Point ;
import java.io.File ;
import java.io.FileFilter ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class GMPImgRefactor {
    
    private static final Logger log = Logger.getLogger( GMPImgRefactor.class ) ;
    
    private File baseDir = new File( "/Users/sandeep/projects/source/SConsoleProcessedImages" ) ;
    
    public void run() {
        File[] dirs = baseDir.listFiles( new FileFilter() {
            public boolean accept( File pathname ) {
                return pathname.isDirectory() && pathname.getName().startsWith( "IIT" ) ;
            }
        } ) ;
        
        for( File dir : dirs ) {
            scanFolder( dir ) ;
        }
    }
    
    private void scanFolder( File folder ) {
        
        File[] files = folder.listFiles( new FileFilter() {
            public boolean accept( File pathname ) {
                return pathname.isDirectory() || pathname.getName().endsWith( ".png" ) ;
            }
        } ) ;
        
        for( File file : files ) {
            if( file.isDirectory() ) {
                scanFolder( file ) ;
            }
            else {
                if( file.getName().contains( "_GMP_" ) ) {
                    processGMPFile( file ) ;
                }
            }
        }
    }
    
    private void processGMPFile( File file ) {
        
        String fileName = file.getName() ;
        fileName = fileName.substring( 0, fileName.lastIndexOf( '.' ) ) ;
        
        if( fileName.contains( "." ) ) {
            changeFile( file ) ;
        }
    }
    
    private void changeFile( File file ) {
        
        Point qNo = parseQNo( file.getName() ) ;
        moveFile( file, qNo ) ;
        if( qNo.y == 1 ) {
            File possibleMissedPart = checkForPossibleMissingStartPart( file, qNo ) ;
            if( possibleMissedPart != null ) {
                Point qNo1 = parseQNo( possibleMissedPart.getName() ) ;
                moveFile( possibleMissedPart, qNo1 ) ;
            }
        }
    }
    
    private Point parseQNo( String fileName ) {
        fileName = fileName.substring( 0, fileName.lastIndexOf( '.' ) ) ;
        String qNoStr = fileName.substring( fileName.lastIndexOf( '_' ) + 1 ) ;
        String[] parts = qNoStr.split( "\\." ) ;
        
        if( parts.length > 1 ) {
            return new Point( Integer.parseInt( parts[0] ), 
                              Integer.parseInt( parts[1] ) ) ;
        }
        return new Point( Integer.parseInt( parts[0] ), 0 ) ;
    }
    
    private File checkForPossibleMissingStartPart( File file, Point qNo ) {
        String fileName = file.getName() ;
        fileName = fileName.substring( 0, fileName.lastIndexOf( '_' ) ) ;
        fileName = fileName + "_" + qNo.x + ".png" ;
        
        File possibleFile = new File( file.getParent(), fileName ) ;
        if( possibleFile.exists() ) {
            return possibleFile ;
        }
        return null ;
    }
    
    private void moveFile( File src, Point qNo ) {
        try {
            String oldFileName = src.getName() ;
            oldFileName = oldFileName.substring( 0, oldFileName.lastIndexOf( '_' ) ) ;
            String newFileName = oldFileName + "_" + qNo.x + "[" + qNo.y + "].png" ;
            log.debug( "Moving - " + src.getName() + " to " + newFileName ) ;
            
            File newFile = new File( src.getParent(), newFileName ) ;
            FileUtils.moveFile( src, newFile ) ;
        }
        catch( Exception e ) {
            e.printStackTrace();
        }
    }

    public static void main( String[] args ) {
        new GMPImgRefactor().run() ;
    }

}
