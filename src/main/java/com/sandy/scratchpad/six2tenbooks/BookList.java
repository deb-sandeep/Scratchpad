package com.sandy.scratchpad.six2tenbooks;

import java.io.BufferedReader ;
import java.io.File ;
import java.io.InputStreamReader ;
import java.util.ArrayList ;
import java.util.Arrays ;
import java.util.Comparator ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.log4j.Logger ;

import com.univocity.parsers.csv.CsvParser ;
import com.univocity.parsers.csv.CsvParserSettings ;
import com.univocity.parsers.csv.CsvWriter ;
import com.univocity.parsers.csv.CsvWriterSettings ;

public class BookList {
    
    private static final Logger log = Logger.getLogger( BookList.class ) ;
    
    private static final File BASE_DIR = new File( "/home/sandeep/temp/icse-ebooks/ICSE Scan Books" ) ;
    
    private File csvFile = new File( BASE_DIR, "ls1.csv" ) ;
    
    private CsvWriter csvWriter = null ;
    private CsvParser csvReader = null ; 
    
    private Map<File, String> xMap = new HashMap<>() ;
    
    public BookList() {
    }
    
    public void doListing( File srcDir ) {
        CsvWriterSettings settings = new CsvWriterSettings() ;
        csvWriter = new CsvWriter( csvFile, settings ) ;
        list( srcDir ) ;
        csvWriter.close() ;
    }
    
    public void list( File srcDir ) {
        File[] files = srcDir.listFiles() ;
        
        Arrays.sort( files, new Comparator<File>() {
            public int compare( File o1, File o2 ) {
                return o1.getName().compareTo( o2.getName() ) ;
            }
        } ) ;
        
        for( File file : files ) {
            if( file.isDirectory() ) {
                list( file ) ;
            }
            else {
                File parentDir = file.getParentFile() ;
                String dirName = parentDir.getAbsolutePath()
                                          .substring( BASE_DIR.getAbsolutePath()
                                                              .length() ) ;
                
                log.debug( dirName + "/" + file.getName() ) ;
                csvWriter.writeRow( dirName, file.getName() ) ;
            }
        }
    }
    
    public void loadLSCsv() {
        CsvParserSettings settings = new CsvParserSettings() ;
        csvReader = new CsvParser( settings ) ;
        csvReader.beginParsing( csvFile ) ;
        
        List<String[]> records = csvReader.parseAll() ;
        
        for( String[] record : records ) {
            File key = new File( BASE_DIR, record[0] + "/" + record[1] ) ;
            String val = record[2] ;
            xMap.put( key, val ) ;
        }
    }
    
    public void renameFilesInDir( File srcDir ) {
        File[] files = srcDir.listFiles() ;
        
        for( File file : files ) {
            if( file.isDirectory() ) {
                renameFilesInDir( file ) ;
            }
            else {
                String newFName = xMap.get( file ) ;
                if( newFName == null ) {
                    log.error( "No mapping found for " + file.getAbsolutePath() ) ;
                }
                else {
                    File newFile = new File( file.getParentFile(), newFName ) ;
                    file.renameTo( newFile ) ;
                    
                    log.debug( "Renamed " + file.getAbsolutePath() + 
                               " to \n\t" + newFile.getAbsolutePath() ) ;
                }
            }
        }
    }
    
    public void imagifyAllPDFs( File dir ) throws Exception {
        
        List<File> filesForImagification = new ArrayList<>() ;
        collectFilesForImagification( dir, filesForImagification ) ;
        
        log.debug( filesForImagification.size() + " files found." ) ;
        for( int i=0; i<filesForImagification.size(); i++ ) {
            File file = filesForImagification.get( i ) ;
            
            log.debug( "Processing " + file.getAbsolutePath() ) ;
            imagifyPDF( file ) ;
            log.debug( "Processing complete. Num files left = " + 
                       (filesForImagification.size()-i-1) ) ;
            System.exit( 0 ) ;
        }
    }
    
    private void collectFilesForImagification( File dir, List<File> collectedFiles ) {
        
        File[] files = dir.listFiles() ;
        
        for( File file : files ) {
            if( file.isDirectory() ) {
                collectFilesForImagification( file, collectedFiles ) ;
            }
            else {
                String fName = file.getName() ;
                if( fName.matches( "[0-9][0-9] - .*" ) && 
                    fName.endsWith( ".pdf" ) ) {
                    collectedFiles.add( file ) ;
                }
            }
        }
    }
    
    private void imagifyPDF( File file ) throws Exception {
        
        String fName = file.getName() ;
        String dirName = fName.substring( 0, fName.length()-4 ) ;
        File dirForFile = new File( file.getParentFile(), dirName ) ;
        
        if( !dirForFile.exists() ) {
            dirForFile.mkdirs() ;
        }
        
        invokeScript( file.getParentFile(), fName, dirName ) ;
    }
    
    private void invokeScript( File wd, String fName, String outputDirName ) 
        throws Exception {
        
        Process p ;
        List<String> cmdList = new ArrayList<String>();
        cmdList.add( "sh" ) ;
        cmdList.add( "/home/sandeep/projects/source/ShellScripts/extract-pdf-images.sh" ) ;
        cmdList.add( wd.getAbsolutePath() ) ;
        cmdList.add( fName ) ;
        cmdList.add( outputDirName ) ;
        
        ProcessBuilder pb = new ProcessBuilder(cmdList);
        p = pb.start();
            
        BufferedReader reader = new BufferedReader( new InputStreamReader(
                                                    p.getInputStream())); 
        String line; 
        while((line = reader.readLine()) != null) { 
            log.debug( line ) ;
        } 
        p.waitFor(); 
    }
    
    public static void main( String[] args ) throws Exception {
        
        BookList app = new BookList() ;
        //app.doListing( BASE_DIR ) ;
        //app.loadLSCsv() ;
        //app.renameFilesInDir( BASE_DIR ) ;
        app.imagifyAllPDFs( BASE_DIR ) ;
    }
}
