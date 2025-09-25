package com.sandy.scratchpad.pdf;


import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class KSVPdfCleaner {
    private static final Logger log = Logger.getLogger( KSVPdfCleaner.class) ;
    
    public static void main( String[] args ) throws IOException {
        
        File srcFile = new File( "/Users/sandeep/temp/book.pdf" ) ;
        File destFile = new File( "/Users/sandeep/temp/book.cleaned.pdf" ) ;
        
        KSVPdfCleaner cleaner = new KSVPdfCleaner( srcFile, destFile ) ;
        cleaner.processSrcPdf() ;
    }
    
    private final File srcFile;
    private final File destFile;
    
    public KSVPdfCleaner( File srcFile, File destFile ) {
        this.srcFile = srcFile;
        this.destFile = destFile;
    }
    
    public void processSrcPdf() throws IOException {
        
        log.debug( "Processing " + srcFile ) ;
        processPdf( this.srcFile, this.destFile ) ;
    }
    
    private void processPdf( File srcFile, File destFile ) throws IOException {
        
        PdfReader   reader = new PdfReader( srcFile ) ;
        PdfWriter   writer = new PdfWriter( destFile ) ;
        
        PdfDocument pdf = new PdfDocument( reader, writer ) ;

        log.debug( "Processing " + srcFile.getName() ) ;
        
        for( int i=1; i<=pdf.getNumberOfPages(); i++ ) {
            log.debug( "  Processing page " + i ) ;
            PdfPage page = pdf.getPage( i ) ;
            //processAnnots( page ) ;
            //processContents( page ) ;
            processResources( page ) ;
        }
        
        log.debug( "  Writing modified pdf" );
        pdf.close() ;
    }
    
    private void processAnnots( PdfPage page ) {
        PdfObject contents = page.getPdfObject().get( PdfName.Annots ) ;
        if( contents instanceof PdfArray ) {
            PdfArray pdfArray = (PdfArray) contents ;
            List<Integer> idxToRemove = new ArrayList<>();
            
            for( int i=0; i<pdfArray.size(); i++ ) {
                idxToRemove.add( i ) ;
            }
            
            if( !idxToRemove.isEmpty() ) {
                idxToRemove.forEach( objId -> {
                    log.debug( "    Removing annotation from page. Obj id = " + objId ) ;
                    pdfArray.remove( objId ) ;
                } ) ;
            }
        }
    }
    
    private void processContents( PdfPage page ) {
        
        PdfObject contents = page.getPdfObject().get( PdfName.Contents ) ;
        
        if( contents instanceof PdfArray ) {
            PdfArray pdfArray = (PdfArray) contents ;
            List<Integer> idxToRemove = new ArrayList<>();
            
            for( int j=0; j<pdfArray.size(); j++ ) {
                PdfObject obj = pdfArray.get( j ) ;
                if( obj instanceof PdfStream ) {
                    PdfStream stream = (PdfStream) obj;
                    String streamContents = new String( stream.getBytes() ) ;
                    
                    if( streamContents.contains( "downloaded" ) ) {
                        idxToRemove.add( j ) ;
                    }
                }
            }
            
            if( !idxToRemove.isEmpty() ) {
                idxToRemove.forEach( objId -> {
                    log.debug( "    Removing footer from page. Obj id = " + objId ) ;
                    pdfArray.remove( objId ) ;
                } ) ;
            }
        }
    }
    
    private void processResources( PdfPage page ) {
        
        PdfDictionary resources = page.getResources().getPdfObject();
        PdfDictionary dict = resources.getAsDictionary( PdfName.XObject ) ;
        if( dict.containsKey( new PdfName( "NxFm0" ) ) ) {
            dict.remove( new PdfName( "NxFm0" ) ) ;
            log.debug( "    Removed NxFm0 from page resources" ) ;
        }
        
        if( dict.containsKey( new PdfName( "NxFm1" ) ) ) {
            dict.remove( new PdfName( "NxFm1" ) ) ;
            log.debug( "    Removed NxFm1 from page resources" ) ;
        }
    }
}
