package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.FileReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Properties;

/**
 * AI processor settings, read from {@code ~/.qclassifier.properties} when it
 * exists. Every key is optional; a JVM system property of the same name
 * (e.g. {@code -Dopenrouter.model=...}) overrides the file.
 *
 * <pre>
 * gemma.enabled          = true
 * gemma.server.url       = http://192.168.0.149:11434/api/generate
 * gemma.model            = gemma4:latest
 * gemma.max.parallel     = 5
 *
 * openrouter.enabled      = true     # default: true if the API key file exists
 * openrouter.model        = google/gemini-2.5-flash
 * openrouter.max.parallel = 5
 * openrouter.api.key.file = ~/.open-router.api.key
 * </pre>
 */
@Slf4j
public final class QClassifierConfig {

    public static final File CONFIG_FILE =
            new File( System.getProperty( "user.home" ), ".qclassifier.properties" ) ;

    private static final String DEFAULT_GEMMA_URL      = "http://192.168.0.149:11434/api/generate" ;
    private static final String DEFAULT_GEMMA_MODEL    = "gemma4:latest" ;
    private static final String DEFAULT_OR_MODEL       = "google/gemini-2.5-flash" ;
    private static final String DEFAULT_OR_KEY_FILE    = "~/.open-router.api.key" ;
    private static final int    DEFAULT_PARALLEL       = 5 ;

    public final boolean gemmaEnabled ;
    public final String  gemmaServerUrl ;
    public final String  gemmaModel ;
    public final int     gemmaMaxParallel ;

    public final boolean openRouterEnabled ;
    public final String  openRouterModel ;
    public final int     openRouterMaxParallel ;
    /** Null when the key file is missing or empty. */
    public final String  openRouterApiKey ;

    private final Properties props = new Properties() ;

    private QClassifierConfig() {
        if( CONFIG_FILE.isFile() ) {
            try( Reader r = new FileReader( CONFIG_FILE, StandardCharsets.UTF_8 ) ) {
                props.load( r ) ;
            }
            catch( Exception e ) {
                log.error( "Error reading {} - using defaults", CONFIG_FILE, e ) ;
            }
        }

        gemmaEnabled     = bool( "gemma.enabled", true ) ;
        gemmaServerUrl   = str( "gemma.server.url", DEFAULT_GEMMA_URL ) ;
        gemmaModel       = str( "gemma.model", DEFAULT_GEMMA_MODEL ) ;
        gemmaMaxParallel = num( "gemma.max.parallel", DEFAULT_PARALLEL ) ;

        openRouterApiKey      = readKey( expandHome( str( "openrouter.api.key.file", DEFAULT_OR_KEY_FILE ) ) ) ;
        openRouterEnabled     = bool( "openrouter.enabled", openRouterApiKey != null ) ;
        openRouterModel       = str( "openrouter.model", DEFAULT_OR_MODEL ) ;
        openRouterMaxParallel = num( "openrouter.max.parallel", DEFAULT_PARALLEL ) ;
    }

    public static QClassifierConfig load() {
        return new QClassifierConfig() ;
    }

    private String str( String key, String def ) {
        String v = System.getProperty( key, props.getProperty( key ) ) ;
        return v == null || v.isBlank() ? def : v.trim() ;
    }

    private boolean bool( String key, boolean def ) {
        String v = str( key, null ) ;
        return v == null ? def : Boolean.parseBoolean( v ) ;
    }

    private int num( String key, int def ) {
        String v = str( key, null ) ;
        if( v == null ) return def ;
        try {
            return Math.max( 1, Integer.parseInt( v ) ) ;
        }
        catch( NumberFormatException e ) {
            log.error( "{} '{}' is not a whole number - using {}", key, v, def ) ;
            return def ;
        }
    }

    private static File expandHome( String path ) {
        if( path.startsWith( "~/" ) ) return new File( System.getProperty( "user.home" ), path.substring( 2 ) ) ;
        return new File( path ) ;
    }

    private static String readKey( File keyFile ) {
        if( !keyFile.isFile() ) return null ;
        try {
            String key = Files.readString( keyFile.toPath(), StandardCharsets.UTF_8 ).trim() ;
            return key.isEmpty() ? null : key ;
        }
        catch( Exception e ) {
            log.error( "Error reading OpenRouter API key from {}", keyFile, e ) ;
            return null ;
        }
    }
}
