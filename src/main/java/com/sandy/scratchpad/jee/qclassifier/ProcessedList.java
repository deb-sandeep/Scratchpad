package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Read / append / rewrite of the processed-sources list file (one source
 * folder name per line). All access is serialised through a single lock so
 * the batch pass and a repair never interleave writes.
 */
@Slf4j
public final class ProcessedList {

    private static final Object LOCK = new Object() ;

    private ProcessedList() {}

    /** Source names in the file, in file order; empty if the file is missing or unreadable. */
    public static Set<String> load( File file ) {
        synchronized( LOCK ) {
            Set<String> names = new LinkedHashSet<>() ;
            if( !file.exists() ) return names ;
            try {
                for( String line : Files.readAllLines( file.toPath(), StandardCharsets.UTF_8 ) ) {
                    String trimmed = line.trim() ;
                    if( !trimmed.isEmpty() ) names.add( trimmed ) ;
                }
            }
            catch( Exception e ) {
                log.error( "Error reading processed list file {}", file, e ) ;
            }
            return names ;
        }
    }

    /** Append {@code srcName} unless it is already listed. */
    public static void append( File file, String srcName ) {
        synchronized( LOCK ) {
            try {
                if( load( file ).contains( srcName ) ) return ;
                Files.writeString( file.toPath(), srcName + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND ) ;
            }
            catch( Exception e ) {
                log.error( "Error appending to processed list file {}", file, e ) ;
            }
        }
    }

    /** Replace the file's contents with {@code srcNames}, one per line. */
    public static void rewrite( File file, Collection<String> srcNames ) throws Exception {
        synchronized( LOCK ) {
            Path tmp = new File( file.getAbsolutePath() + ".tmp" ).toPath() ;
            StringBuilder sb = new StringBuilder() ;
            for( String n : srcNames ) sb.append( n ).append( System.lineSeparator() ) ;
            Files.writeString( tmp, sb.toString(), StandardCharsets.UTF_8 ) ;
            Files.move( tmp, file.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE ) ;
        }
    }
}
