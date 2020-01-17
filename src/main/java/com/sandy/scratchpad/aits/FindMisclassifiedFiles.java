package com.sandy.scratchpad.aits;

import java.io.File ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class FindMisclassifiedFiles {

    private static final Logger log = Logger.getLogger( FindMisclassifiedFiles.class ) ;
    
    private static final File ROOT_DIR = new File( "/home/sandeep/projects/source/SConsoleProcessedImages/IIT - Physics" ) ;
    
    private static final String[] MISSING_FILES = {
        "Phy_Q_FJ14_CRS1A2_LCT_1.png", 
        "Phy_Q_FJ14_CRS1A2_LCT_2.png", 
        "Phy_Q_FJ14_CRS2A2_LCT_1.png", 
        "Phy_Q_FJ14_CRS2A2_LCT_2.png", 
        "Phy_Q_FJ14_CRS3A1_LCT_1.png", 
        "Phy_Q_FJ14_CRS4A2_LCT_1.png", 
        "Phy_Q_FJ14_FTS1A1_LCT_1.png", 
        "Phy_Q_FJ14_FTS1A2_LCT_1.png", 
        "Phy_Q_FJ14_FTS1A2_LCT_2.png", 
        "Phy_Q_FJ14_FTS2A1_LCT_2.png", 
        "Phy_Q_FJ14_FTS2A2_LCT_1.png", 
        "Phy_Q_FJ14_FTS2A2_LCT_2.png", 
        "Phy_Q_FJ14_FTS4A1_LCT_1.png", 
        "Phy_Q_FJ14_FTS4A1_LCT_2.png", 
        "Phy_Q_FJ14_FTS5A1_LCT_1.png", 
        "Phy_Q_FJ14_FTS5A1_LCT_2.png", 
        "Phy_Q_FJ14_FTS5A2_LCT_1.png", 
        "Phy_Q_FJ14_FTS5A2_LCT_2.png" 
    } ;
    
    public void search( File dir ) throws Exception {
        File[] files = dir.listFiles() ;
        for( File file : files ) {
            String fileName = file.getName() ;
            if( file.isDirectory() ) {
                search( file ) ;
            }
            else if( fileName.endsWith( ".png" ) ) {
                for( String missingFileName : MISSING_FILES ) {
                    if( fileName.equals( missingFileName ) ) {
                        FileUtils.moveFile( file, new File( ROOT_DIR, missingFileName ) ) ;
                        break ;
                    }
                }
            }
        }
    }
    
    public static void main( String[] args ) throws Exception {
        FindMisclassifiedFiles finder = new FindMisclassifiedFiles() ;
        finder.search( ROOT_DIR ) ;
    }
}
