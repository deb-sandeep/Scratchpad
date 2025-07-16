package com.sandy.scratchpad.pdf;


import com.itextpdf.kernel.pdf.*;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class JDLeePdfCleaner {
    private static final Logger log = Logger.getLogger( JDLeePdfCleaner.class) ;
    
    public static void main( String[] args ) throws IOException {
        
        File srcFile = new File( "/Users/sandeep/Downloads/JDLee.pdf" ) ;
        File destFile = new File( "/Users/sandeep/Downloads/JDLee.cleaned.pdf" ) ;
        
        JDLeePdfCleaner cleaner = new JDLeePdfCleaner( srcFile, destFile ) ;
        cleaner.processSrcPdf() ;
    }
    
    private final File srcFile;
    private final File destFile;
    
    public JDLeePdfCleaner( File srcFile, File destFile ) {
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
            
            PdfObject contents = page.getPdfObject().get( PdfName.Contents ) ;
            
            if( contents instanceof PdfArray ) {
                PdfArray pdfArray = (PdfArray) contents ;
                List<Integer> idxToRemove = new ArrayList<>();
                
                for( int j=0; j<pdfArray.size(); j++ ) {
                    PdfObject obj = pdfArray.get( j ) ;
                    if( obj instanceof PdfStream ) {
                        PdfStream stream = (PdfStream) obj;
                        String streamContents = new String( stream.getBytes() ) ;
                        
                        if( streamContents.contains( "@edubuzznotes" ) ) {
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
        
        log.debug( "  Writing modified pdf" );
        pdf.close() ;
    }
}
