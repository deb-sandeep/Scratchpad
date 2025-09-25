package com.sandy.scratchpad.pdf;


import com.itextpdf.kernel.pdf.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JEEPdfCleaner {
    private static final Logger log = Logger.getLogger( JEEPdfCleaner.class) ;
    
    public static void main( String[] args ) throws IOException {
        
        File srcDir = new File( "/Users/sandeep/temp/akash/JEE-Archive" ) ;
        File destDir = new File( "/Users/sandeep/temp/akash/JEE-Archive-Cleaned" ) ;
        
        JEEPdfCleaner cleaner = new JEEPdfCleaner( srcDir, destDir ) ;
        cleaner.processSrcPdfs( srcDir ) ;
    }
    
    private final File srcDir ;
    private final File destDir ;
    
    public JEEPdfCleaner( File srcDir, File destDir ) {
        this.srcDir = srcDir ;
        this.destDir = destDir ;
    }
    
    public void processSrcPdfs( File dir ) throws IOException {
        
        log.debug( "Processing " + dir.getAbsolutePath() ) ;
        for( File file : Objects.requireNonNull( dir.listFiles() ) ) {
            if( !file.isDirectory() ) {
                if( file.getName().endsWith(".pdf") ) {
                    String fileName = file.getName() ;
                    File destFile = getDestFile( file ) ;
                    processPdf( file, destFile ) ;
                    log.debug( "Processing " + fileName ) ;
                }
            }
            else {
                processSrcPdfs( file ) ;
            }
        }
    }
    
    private File getDestFile( File srcFile ) {
        String filePath = srcFile.getAbsolutePath() ;
        String destPath = filePath.replace( this.srcDir.getAbsolutePath(), this.destDir.getAbsolutePath() ) ;
        File destFile = new File( destPath ) ;
        if( !destFile.getParentFile().exists() ) {
            destFile.getParentFile().mkdirs() ;
        }
        return destFile ;
    }
    
    private static void processPdf( File srcFile, File destFile ) throws IOException {
        
        log.debug( "Processing " + srcFile.getName() ) ;
        
        PdfReader reader = new PdfReader( srcFile ) ;
        PdfWriter writer = new PdfWriter( destFile ) ;
        
        PdfDocument pdf = new PdfDocument( reader, writer ) ;

        for( int i=1; i<=pdf.getNumberOfPages(); i++ ) {
            log.debug( "  Processing page " + i ) ;
            PdfPage page = pdf.getPage( i ) ;
            
            PdfDictionary resources   = page.getResources().getPdfObject();
            PdfDictionary xobjects = resources.getAsDictionary(PdfName.XObject);
            List<PdfName> keysToRemove = new ArrayList<>();
            
            if (xobjects != null) {
                for( PdfName name : xobjects.keySet() ) {
                    if( name.toString().equals( "/Fm0" ) ) {
                        PdfStream stream = xobjects.getAsStream( name ) ;
                        if( stream.containsKey( new PdfName( "PieceInfo" ) ) ) {
                            PdfDictionary pieceInfo = stream.getAsDictionary( new PdfName( "PieceInfo" ) ) ;
                            if( pieceInfo.containsKey( new PdfName( "ADBE_CompoundType" ) ) ) {
                                keysToRemove.add( name ) ;
                            }
                        }
                    }
                    /*
                    PdfStream xobj = xobjects.getAsStream(name);
                    if (xobj != null && PdfName.Image.equals(xobj.getAsName(PdfName.Subtype))) {
                        // Heuristic: if image size/opacity suggests watermark, remove
                        xobjects.remove(name);
                        System.out.println("Removed XObject " + name + " on page " + i);
                    }
                     */
                }
                
                if( !keysToRemove.isEmpty() ) {
                    for( PdfName name : keysToRemove ) {
                        log.debug( "       Removed watermark" ) ;
                        xobjects.remove( name ) ;
                    }
                }
            }
        }
        
        log.debug( "  Writing modified pdf" );
        pdf.close() ;
    }
}
