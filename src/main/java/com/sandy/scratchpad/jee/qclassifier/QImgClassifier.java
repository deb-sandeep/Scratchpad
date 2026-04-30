package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
public class QImgClassifier {

    public static final File Q_BANK_DIR = new File( "/Users/sandeep/Documents/StudyNotes/question-bank" ) ;
    public static final String GEMMA_SERVER_URL = "http://192.168.0.149:11434/api/generate" ;
    public static final String GEMMA_MODEL = "gemma4:latest" ;

    public static final File PROCESSED_LIST_FILE =
            new File( System.getProperty( "user.home" ), ".qclassifier.processed.list" ) ;
    public static final String SOURCES_RESOURCE = "/qclassifier.sources.txt" ;

    public static void main( String[] args ) throws Exception {
        QImgClassifier app = new QImgClassifier() ;
        app.runClassification() ;
    }

    private static final int MAX_PARALLEL_THREADS = 4 ;
    private static final String Q_ID = "" ;

    public QImgClassifier() {
    }

    private void runClassification() throws Exception{
        Set<String> processed = loadProcessedList() ;
        List<String> sources = loadSourceList() ;

        ExecutorService executor = Executors.newFixedThreadPool( MAX_PARALLEL_THREADS ) ;
        GemmaInvoker gemmaInvoker = new GemmaInvoker() ;
        for( String srcName : sources ) {
            if( processed.contains( srcName ) ) {
                log.debug( "Skipping already processed source - {}", srcName ) ;
                continue ;
            }
            File srcDir = FileHelper.findDir( Q_BANK_DIR, srcName ) ;
            if( srcDir == null ) {
                log.error( "Source directory {} not found", srcName ) ;
                continue ;
            }
            executor.submit( new QSourceClassifier( srcDir, Q_ID, gemmaInvoker,
                                                    PROCESSED_LIST_FILE ) ) ;
            Thread.sleep( 500 ) ;
        }
        executor.shutdown() ;
        try {
            executor.awaitTermination( Long.MAX_VALUE, TimeUnit.SECONDS ) ;
        }
        catch( InterruptedException e ) {
        log.error( "Classification interrupted", e ) ;
            Thread.currentThread().interrupt() ;
        }
    }

    private Set<String> loadProcessedList() {
        Set<String> processed = new HashSet<>() ;
        if( !PROCESSED_LIST_FILE.exists() ) return processed ;
        try {
            for( String line : Files.readAllLines( PROCESSED_LIST_FILE.toPath(), StandardCharsets.UTF_8 ) ) {
                String trimmed = line.trim() ;
                if( !trimmed.isEmpty() ) processed.add( trimmed ) ;
            }
        }
        catch( Exception e ) {
            log.error( "Error reading processed list file {}", PROCESSED_LIST_FILE, e ) ;
        }
        return processed ;
    }

    private List<String> loadSourceList() {
        try( InputStream is = getClass().getResourceAsStream( SOURCES_RESOURCE ) ) {
            if( is == null ) {
                log.error( "Sources resource {} not found", SOURCES_RESOURCE ) ;
                return List.of() ;
            }
            String content = new String( is.readAllBytes(), StandardCharsets.UTF_8 ) ;
            return content.lines()
                    .map( String::trim )
                    .filter( s -> !s.isEmpty() )
                    .collect( Collectors.toList() ) ;
        }
        catch( Exception e ) {
            log.error( "Error loading sources resource {}", SOURCES_RESOURCE, e ) ;
            return List.of() ;
        }
    }
}
