package com.sandy.scratchpad.yg20;

import java.io.File ;
import java.io.FilenameFilter ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class YG20QProcessor {

    private static final Logger log = Logger.getLogger( YG20QProcessor.class ) ;
    
    private static File IMG_DIR      = new File( "/Users/sandeep/temp/question-clips" ) ;
    private static File TGT_DIR_ROOT = new File( "/Users/sandeep/projects/source/SConsoleProcessedImages" ) ;
    private static File TGT_PHY      = new File( TGT_DIR_ROOT, "IIT - Physics" ) ;
    private static File TGT_CHEM     = new File( TGT_DIR_ROOT, "IIT - Chemistry" ) ;
    private static File TGT_MATH     = new File( TGT_DIR_ROOT, "IIT - Maths" ) ;

    private List<QMeta> metaList = null ;
    
    public static void main( String[] args ) throws Exception {
        log.debug( "Starting YG 20 question processor..." ) ;
        YG20QProcessor processor = new YG20QProcessor() ;
        processor.loadMetaData() ;
        processor.moveLCTImages() ;
        processor.processQuestionImages() ;
        processor.writeAnsLookup() ;
        //processor.listMMT() ;
        log.debug( "YG20QProcessor ended." ) ;
    }
    
    public void loadMetaData() {
        log.debug( "Loading meta data" ) ;
        QMetaParser parser = new QMetaParser() ;
        metaList = parser.parseQuestionMeta() ;
    }
    
    public void moveLCTImages() throws Exception {
        log.debug( "Moving LCT images..." ) ;
        File[] files = IMG_DIR.listFiles() ;
        for( File file : files ) {
            String fileName = file.getName() ;
            if( fileName.contains( "_LCT_" ) ) {
                log.debug( "\tMoving - " + fileName ) ;
                File tgtDir = getTgtDir( file ) ;
                FileUtils.moveFileToDirectory( file, tgtDir, false ) ;
            }
        }
    }
    
    public void processQuestionImages() throws Exception {
        log.debug( "Processing question images..." ) ;
        for( QMeta meta : metaList ) {
            if( meta.qType.equals( "ART" ) ||
                meta.qType.equals( "SUB" ) ||
                meta.qType.equals( "LCT" ) ||
                meta.qType.equals( "MMT" ) ) {
                continue ;
            }
            else {
                File[] files = findMatchingImgFiles( meta ) ;
                for( File file : files ) {
                    moveQuestionImage( file, meta ) ;
                }
            }
        }
    }
    
    private File[] findMatchingImgFiles( QMeta meta ) {
        final String filePrefix = meta.sub + "_Q_YG201_" + meta.qNo ;
        File[] files = IMG_DIR.listFiles( new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                return name.startsWith( filePrefix + "." ) || 
                       name.startsWith( filePrefix + "(" ) ;
            }
        } ) ;
        return files ;
    }
    
    private void moveQuestionImage( File srcFile, QMeta meta ) 
        throws Exception {
        
        String srcFN = srcFile.getName() ;
        String[] srcFNParts = srcFN.split( "_" ) ;
        String destFN = srcFNParts[0] + 
                        "_Q_YG201_" + 
                        meta.qType + "_" + 
                        srcFNParts[3] ;
        
        File tgtDir = getTgtDir( srcFile ) ;
        File destFile = new File( tgtDir, destFN ) ;
        log.debug( "\t" + srcFN + " > " + destFN ) ; 
        FileUtils.moveFile( srcFile, destFile ) ;
    }
    
    private File getTgtDir( File srcFile ) {
        String fileName = srcFile.getName() ;
        if( fileName.startsWith( "Phy_" ) ) {
            return TGT_PHY ;
        }
        else if( fileName.startsWith( "Chem_" ) ) {
            return TGT_CHEM ;
        }
        else if( fileName.startsWith( "Math_" ) ) {
            return TGT_MATH ;
        }
        throw new IllegalArgumentException( "Illegal image file " + fileName ) ;
    }

    public void writeAnsLookup() {
        for( QMeta meta : metaList ) {
            if( meta.qType.equals( "ART" ) ||
                meta.qType.equals( "SUB" ) || 
                meta.qType.equals( "MMT" ) ) {
                continue ;
            }
            String key = meta.sub + "_Q_YG201_" + meta.qType + "_" + meta.qNo ;
            log.debug( key + "=" + meta.ans ) ;
        }
    }
}
