package com.sandy.scratchpad.evtdoc;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.sandy.scratchpad.evtdoc.collector.EventSrcCollector;
import com.sandy.scratchpad.evtdoc.collector.EventTgtCollector;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;

public class EventDocApp {
    
    private static final Logger log = Logger.getLogger( EventDocApp.class ) ;
    
    private static final File SRC_DIR = new File( "/Users/sandeep/projects/sconsole/SConsoleNxt/src/main/java" ) ;
    
    public static void main( String[] args ) throws Exception {
        StaticJavaParser.getParserConfiguration().setLanguageLevel( ParserConfiguration.LanguageLevel.JAVA_21 ) ;
        EventDocApp app = new EventDocApp() ;
        app.parseSrcFileDir( SRC_DIR) ;
        app.runDocumentor() ;
    }
    
    public void parseSrcFileDir( File dir ) {
        for( File file : Objects.requireNonNull( dir.listFiles() ) ) {
            if( file.isDirectory() ) {
                parseSrcFileDir( file ) ;
            }
            else if( isJavaFile( file.toPath() ) ) {
                parseJavaFile( file ) ;
            }
        }
    }
    
    private boolean isJavaFile( Path path ) {
        return path.toString().toLowerCase().endsWith( ".java" );
    }
    
    private void parseJavaFile( File file ) {
        
        try {
            EventSrcCollector evtSrcCollector = new EventSrcCollector() ;
            EventTgtCollector evtTgtCollector = new EventTgtCollector() ;
            
            CompilationUnit cu = StaticJavaParser.parse( file ) ;
            
            evtSrcCollector.visit( cu, null ) ;
            evtTgtCollector.visit( cu, null ) ;
            evtTgtCollector.cuVisitComplete() ;
        }
        catch( Exception e ) {
            log.error( "Parse failure : " + e.getMessage() ) ;
        }
    }
    
    public void runDocumentor() throws IOException {
        Documenter doc = new Documenter() ;
        doc.generateDocumentation() ;
    }
}
