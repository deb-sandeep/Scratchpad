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
        
        File srcDir = new File( "/Users/sandeep/Documents/StudyNotes/JEE/res/test-papers/Prev JEE Mains" ) ;
        File destDir = new File( "/Users/sandeep/Documents/StudyNotes/JEE/res/test-papers/Prev JEE Mains/out" ) ;
        
        JEEPdfCleaner cleaner = new JEEPdfCleaner( srcDir, destDir ) ;
        cleaner.processSrcPdfs() ;
    }
    
    private final File srcDir ;
    private final File destDir ;
    
    public JEEPdfCleaner( File srcDir, File destDir ) {
        this.srcDir = srcDir ;
        this.destDir = destDir ;
    }
    
    public void processSrcPdfs() throws IOException {
        
        for( File file : Objects.requireNonNull( this.srcDir.listFiles() ) ) {
            if( !file.isDirectory() ) {
                if( file.getName().endsWith(".pdf") ) {
                    String fileName = file.getName() ;
                    File destFile = new File( destDir, fileName ) ;
                    processPdf( file, destFile ) ;
                    log.debug( "Processing " + fileName ) ;
                }
            }
        }
    }
    
    private static void processPdf( File srcFile, File destFile ) throws IOException {
        
        PdfReader   reader = new PdfReader( srcFile ) ;
        PdfWriter   writer = new PdfWriter( destFile ) ;
        
        PdfDocument pdf = new PdfDocument( reader, writer ) ;

        log.debug( "Processing " + srcFile.getName() ) ;
        
        for( int i=1; i<=pdf.getNumberOfPages(); i++ ) {
            log.debug( "  Processing page " + i ) ;
            PdfPage page = pdf.getPage( i ) ;
            
            PdfDictionary resources = page.getResources().getPdfObject();
            PdfDictionary patternDict = resources.getAsDictionary(PdfName.Pattern);
            PdfObject contents = page.getPdfObject().get( PdfName.Contents ) ;
            
            // Clear the background pattern.
            if( patternDict != null && !patternDict.isEmpty() ) {
                log.debug( "    Removing background" ) ;
                patternDict.clear();
            }
            
            if( contents instanceof PdfArray ) {
                PdfArray pdfArray = (PdfArray) contents ;
                List<Integer> idxToRemove = new ArrayList<>();
                
                for( int j=0; j<pdfArray.size(); j++ ) {
                    PdfObject obj = pdfArray.get( j ) ;
                    if( obj instanceof PdfStream ) {
                        PdfStream stream = (PdfStream) obj;
                        String streamContents = new String( stream.getBytes() ) ;
                        
                        if( streamContents.contains( "@JEEAdvanced_" ) ||
                            streamContents.contains( "@FIITJEE" ) ) {
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
