package com.sandy.scratchpad.geogebra.cmdleech;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateExceptionHandler;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

public class TemplateEngine {
    
    private static final Configuration CFG = new Configuration( Configuration.VERSION_2_3_34 ) ;
    private static final File SRC_DIR    = new File( "/Users/sandeep/projects/workspace/geogebra" ) ;
    private static final File TARGET_DIR = new File( "/Users/sandeep/Documents/StudyNotes/JEE/res/docs/GeoGebraCommands" ) ;
    
    static {
        try {
            CFG.setDirectoryForTemplateLoading( SRC_DIR ) ;
            CFG.setDefaultEncoding( "UTF-8" ) ;
            CFG.setTemplateExceptionHandler( TemplateExceptionHandler.RETHROW_HANDLER ) ;
            CFG.setLogTemplateExceptions( true ) ;
            CFG.setWrapUncheckedExceptions( true ) ;
            CFG.setFallbackOnNullLoopVariable( false ) ;
            CFG.setSQLDateAndTimeTimeZone( TimeZone.getDefault());
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }
    
    private File getTargetFile( String targetPath ) {
        File targetFile = new File( TARGET_DIR, targetPath ) ;
        File targetDir = targetFile.getParentFile() ;
        if( !targetDir.exists() ) {
            targetDir.mkdirs() ;
        }
        return targetFile ;
    }
    
    public Map<String, Object> getBaseModel( String title ) {
        Map<String, Object> model = new HashMap<>() ;
        model.put( "title", title ) ;
        return model ;
    }
    
    public File processTemplate( String templatePath, String targetPath, Map<String, Object> model ) throws Exception {
        Template template = CFG.getTemplate( templatePath ) ;
        File targetFile = getTargetFile( targetPath ) ;
        
        template.process( model, new FileWriter( targetFile ) ) ;
        return targetFile ;
    }
    
    public File processTemplate( String templatePath, String targetPath ) throws Exception {
        return processTemplate( templatePath, targetPath, new HashMap<>() ) ;
    }
    
    public File copyStatic( String staticFilePath, String targetPath ) throws Exception {
        File srcFile = new File( SRC_DIR, staticFilePath ) ;
        File targetFile = getTargetFile( targetPath ) ;
        FileUtils.copyFile( srcFile, targetFile ) ;
        return targetFile ;
    }
    
    public void copyDirectory( String srcDirPath, String targetDirPath ) throws Exception {
        File srcDir = new File( SRC_DIR, srcDirPath ) ;
        File targetDir = new File( TARGET_DIR, targetDirPath ) ;
        FileUtils.copyDirectory( srcDir, targetDir ) ;
    }
}
