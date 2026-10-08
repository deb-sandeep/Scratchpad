package com.sandy.scratchpad.pdf;


import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.pdf.xobject.PdfImageXObject;
import org.slf4j.LoggerFactory;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Removes the "Ananth Garg" watermark from the scanned Cengage KS Verma books.
 *
 * Unlike the other cleaners, the watermark here is not a separate content
 * stream - it is baked into the full page scan image. The watermark pixels
 * are very light with a faint blue tint, while the text is neutral grey/black.
 *
 * For each book, say "Cengage - KS Verma - Inorganic - 1.PDF", each page
 * image is extracted, cleaned and saved as
 * ~/temp/inorganic-1/pages/page-xxx.jpg
 * (the cover page has no watermark and is saved untouched).
 *
 * Pages already present in the pages directory are skipped, so an
 * interrupted run can be resumed.
 */
public class KSVPdfCleaner2 {

    private static final File   BASE_DIR    = new File( "/Users/sandeep/temp" ) ;
    private static final String BOOK_PREFIX = "Cengage - KS Verma - " ;

    // Pixels whose darkest channel is at least this are treated as paper
    private static final int NEAR_WHITE_MIN = 235 ;

    // Light pixels (darkest channel at least this) with a blue tint of at
    // least BLUE_TINT_MIN (blue - red) are treated as watermark
    private static final int TINTED_LIGHT_MIN = 215 ;
    private static final int BLUE_TINT_MIN    = 3 ;

    private static final float JPEG_QUALITY = 0.9f ;

    // Page images with this name prefix are watermarked scans. Anything
    // else (the cover) is saved as is.
    private static final String SCAN_IMG_PREFIX = "fzImg" ;

    // How often the status table gets a new row, and how many rows between headers
    private static final int STATUS_INTERVAL_SECS = 10 ;
    private static final int ROWS_PER_HEADER      = 20 ;

    public static void main( String[] args ) throws IOException {

        // iText logs an INFO for every scanned page saying it leaves the DCT
        // (JPEG) data undecoded - which is exactly what we want. Silence it.
        org.slf4j.Logger dctLogger = LoggerFactory.getLogger( "com.itextpdf.kernel.pdf.filters.DctDecodeFilter" ) ;
        if( dctLogger instanceof ch.qos.logback.classic.Logger ) {
            ((ch.qos.logback.classic.Logger) dctLogger).setLevel( ch.qos.logback.classic.Level.WARN ) ;
        }

        File[] srcFiles ;
        if( args.length > 0 ) {
            srcFiles = Arrays.stream( args ).map( File::new ).toArray( File[]::new ) ;
        }
        else {
            srcFiles = BASE_DIR.listFiles( f -> f.isFile() &&
                                                f.getName().startsWith( BOOK_PREFIX ) &&
                                                f.getName().toLowerCase().endsWith( ".pdf" ) ) ;
            if( srcFiles == null ) {
                srcFiles = new File[0] ;
            }
            Arrays.sort( srcFiles ) ;
        }

        // Set up all books before starting any threads, so a bad file name
        // is reported up front rather than leaving a half started pool
        List<KSVPdfCleaner2> cleaners = new ArrayList<>() ;
        for( File srcFile : srcFiles ) {
            try {
                cleaners.add( new KSVPdfCleaner2( srcFile ) ) ;
            }
            catch( IllegalArgumentException e ) {
                System.out.println( "Skipping " + srcFile.getName() + " : " + e.getMessage() ) ;
            }
        }
        if( cleaners.isEmpty() ) {
            System.out.println( "No books to process" ) ;
            return ;
        }

        // One thread per book. Each book holds only one decoded page in
        // memory at a time, so this is CPU bound.
        int numThreads = Math.min( cleaners.size(), Runtime.getRuntime().availableProcessors() ) ;
        System.out.println( "Processing " + cleaners.size() + " books on " + numThreads + " threads" ) ;

        StatusTable statusTable = new StatusTable( cleaners ) ;
        ExecutorService executor = Executors.newFixedThreadPool( numThreads ) ;
        ScheduledExecutorService ticker = Executors.newSingleThreadScheduledExecutor( r -> {
            Thread t = new Thread( r, "status-table" ) ;
            t.setDaemon( true ) ;
            return t ;
        } ) ;

        List<Throwable> failures = new ArrayList<>() ;
        try {
            List<Future<?>> futures = new ArrayList<>() ;
            for( KSVPdfCleaner2 cleaner : cleaners ) {
                futures.add( executor.submit( () -> {
                    cleaner.process() ;
                    return null ;
                } ) ) ;
            }
            ticker.scheduleAtFixedRate( statusTable::printRow, 0, STATUS_INTERVAL_SECS, TimeUnit.SECONDS ) ;

            // A failed book is reported but doesn't stop the others
            for( Future<?> future : futures ) {
                try {
                    future.get() ;
                }
                catch( ExecutionException e ) {
                    failures.add( e.getCause() ) ;
                }
            }
        }
        catch( InterruptedException e ) {
            Thread.currentThread().interrupt() ;
        }
        finally {
            ticker.shutdownNow() ;
            executor.shutdownNow() ;
        }

        statusTable.printRow() ;
        for( Throwable failure : failures ) {
            System.out.println() ;
            failure.printStackTrace( System.out ) ;
        }
        System.out.println( ( cleaners.size() - failures.size() ) + "/" + cleaners.size() + " books done" ) ;
    }

    /**
     * Prints the progress of all books as a table, one column per book and
     * one row per tick. The header is repeated every ROWS_PER_HEADER rows.
     */
    private static class StatusTable {

        private final List<KSVPdfCleaner2> cleaners ;
        private final long                 startTime = System.currentTimeMillis() ;
        private final String rowFormat ;
        private int numRows = 0 ;

        StatusTable( List<KSVPdfCleaner2> cleaners ) {
            this.cleaners = cleaners ;

            StringBuilder fmt = new StringBuilder( "%-8s" ) ;
            for( KSVPdfCleaner2 cleaner : cleaners ) {
                fmt.append( " | %-" ).append( Math.max( cleaner.bookName.length(), 12 ) ).append( "s" ) ;
            }
            this.rowFormat = fmt.append( "%n" ).toString() ;
        }

        synchronized void printRow() {

            if( numRows % ROWS_PER_HEADER == 0 ) {
                Object[] header = new Object[cleaners.size()+1] ;
                header[0] = "Elapsed" ;
                for( int i=0; i<cleaners.size(); i++ ) {
                    header[i+1] = cleaners.get( i ).bookName ;
                }
                System.out.println() ;
                System.out.printf( rowFormat, header ) ;
            }

            long secs = ( System.currentTimeMillis() - startTime ) / 1000 ;
            Object[] row = new Object[cleaners.size()+1] ;
            row[0] = String.format( "%02d:%02d", secs/60, secs%60 ) ;
            for( int i=0; i<cleaners.size(); i++ ) {
                row[i+1] = cleaners.get( i ).status ;
            }
            System.out.printf( rowFormat, row ) ;
            numRows++ ;
        }
    }

    private final File srcFile;
    private final String bookName;
    private final File bookDir;
    private final File pagesDir;

    // Shown in the status table: waiting, n/total, done Ns or FAILED
    private volatile String status = "waiting" ;

    public KSVPdfCleaner2( File srcFile ) {
        this.srcFile = srcFile;
        this.bookName = getBookName( srcFile ) ;
        this.bookDir = new File( srcFile.getParentFile(), bookName ) ;
        this.pagesDir = new File( bookDir, "pages" ) ;
    }

    public void process() throws IOException {

        long startTime = System.currentTimeMillis() ;
        try {
            status = "starting" ;
            if( !pagesDir.exists() && !pagesDir.mkdirs() ) {
                throw new IOException( "Could not create " + pagesDir ) ;
            }
            extractPages() ;
        }
        catch( IOException | RuntimeException e ) {
            status = "FAILED" ;
            throw e ;
        }
        status = "done " + ( System.currentTimeMillis() - startTime ) / 1000 + "s" ;
    }

    // "Cengage - KS Verma - Inorganic - 1.PDF" -> "inorganic-1"
    private static String getBookName( File srcFile ) {

        String name = srcFile.getName() ;
        int dotIdx = name.lastIndexOf( '.' ) ;
        if( dotIdx > 0 ) {
            name = name.substring( 0, dotIdx ) ;
        }

        String[] tokens = name.split( "-" ) ;
        if( tokens.length < 4 ) {
            throw new IllegalArgumentException( "Can't derive book name from " + srcFile.getName() ) ;
        }
        return ( tokens[2].trim() + "-" + tokens[3].trim() ).toLowerCase() ;
    }

    private void extractPages() throws IOException {

        try( PdfDocument pdf = new PdfDocument( new PdfReader( srcFile ) ) ) {
            int numPages = pdf.getNumberOfPages() ;
            for( int i=1; i<=numPages; i++ ) {
                status = i + "/" + numPages ;
                File pageFile = new File( pagesDir, String.format( "page-%03d.jpg", i ) ) ;
                if( pageFile.exists() ) {
                    continue ;
                }

                byte[] jpegBytes = getPageJpeg( pdf.getPage( i ) ) ;

                // Write via a temp file so an interrupted run never leaves a
                // half written page behind which would be skipped on resume
                File tmpFile = new File( pagesDir, pageFile.getName() + ".tmp" ) ;
                Files.write( tmpFile.toPath(), jpegBytes ) ;
                Files.move( tmpFile.toPath(), pageFile.toPath(), StandardCopyOption.REPLACE_EXISTING ) ;
            }
        }
    }

    private byte[] getPageJpeg( PdfPage page ) throws IOException {

        PdfDictionary xObjects = page.getResources().getPdfObject().getAsDictionary( PdfName.XObject ) ;
        if( xObjects != null ) {
            for( PdfName name : xObjects.keySet() ) {
                PdfStream stream = xObjects.getAsStream( name ) ;
                if( stream == null || !PdfName.Image.equals( stream.getAsName( PdfName.Subtype ) ) ) {
                    continue ;
                }

                PdfImageXObject image = new PdfImageXObject( stream ) ;
                if( name.getValue().startsWith( SCAN_IMG_PREFIX ) ) {
                    BufferedImage bufImg = image.getBufferedImage() ;
                    removeWatermark( bufImg ) ;
                    return toJpeg( bufImg ) ;
                }

                if( "jpg".equals( image.identifyImageFileExtension() ) ) {
                    return image.getImageBytes() ;
                }
                return toJpeg( image.getBufferedImage() ) ;
            }
        }
        throw new IOException( "No image found on page " + page.getDocument().getPageNumber( page ) ) ;
    }

    private int removeWatermark( BufferedImage image ) {

        int numCleaned = 0 ;
        for( int y=0; y<image.getHeight(); y++ ) {
            for( int x=0; x<image.getWidth(); x++ ) {
                int rgb = image.getRGB( x, y ) ;
                int r = (rgb >> 16) & 0xFF ;
                int g = (rgb >>  8) & 0xFF ;
                int b =  rgb        & 0xFF ;
                int min = Math.min( r, Math.min( g, b ) ) ;

                boolean nearWhite = min >= NEAR_WHITE_MIN ;
                boolean blueTinted = min >= TINTED_LIGHT_MIN && ( b - r ) >= BLUE_TINT_MIN ;

                if( ( nearWhite || blueTinted ) && rgb != 0xFFFFFFFF ) {
                    image.setRGB( x, y, 0xFFFFFF ) ;
                    numCleaned++ ;
                }
            }
        }
        return numCleaned ;
    }

    private byte[] toJpeg( BufferedImage image ) throws IOException {

        BufferedImage rgbImage = image ;
        if( image.getType() != BufferedImage.TYPE_INT_RGB ) {
            rgbImage = new BufferedImage( image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_RGB ) ;
            rgbImage.getGraphics().drawImage( image, 0, 0, null ) ;
        }

        ImageWriter jpegWriter = ImageIO.getImageWritersByFormatName( "jpeg" ).next() ;
        ImageWriteParam param = jpegWriter.getDefaultWriteParam() ;
        param.setCompressionMode( ImageWriteParam.MODE_EXPLICIT ) ;
        param.setCompressionQuality( JPEG_QUALITY ) ;

        ByteArrayOutputStream bos = new ByteArrayOutputStream() ;
        try( ImageOutputStream ios = ImageIO.createImageOutputStream( bos ) ) {
            jpegWriter.setOutput( ios ) ;
            jpegWriter.write( null, new IIOImage( rgbImage, null, null ), param ) ;
        }
        finally {
            jpegWriter.dispose() ;
        }
        return bos.toByteArray() ;
    }
}
