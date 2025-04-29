package com.sandy.scratchpad.geogebra.cmdleech;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class Command {
    
    private static final Logger log = Logger.getLogger( Command.class ) ;
    
    private final File file ;
    
    final String cmdName ;
    final String groupName ;
    
    // Derived view attributes
    final String fileName ;
    final String smallGroupName ;
    
    Command( String groupName, File cmdFile ) throws Exception {
        
        String fileName = cmdFile.getName() ;
        
        this.file = cmdFile ;
        this.groupName = groupName ;
        this.cmdName = fileName.substring( 0, fileName.lastIndexOf(".") ) ;
        
        this.fileName = fileName ;
        this.smallGroupName = groupName.substring( 0, groupName.lastIndexOf( ' ' ) ) ;
    }
    
    String getAnchorTag( boolean includeGroupName ) {
        String groupPart = includeGroupName ? groupName + "/" : "" ;
        String groupSuffix = includeGroupName ? "<span class=\"sm-grp-name\">[" + groupName.substring( 0, groupName.lastIndexOf( ' ' ) ) + "]</span>" : "" ;
        return "<a href=\"" + groupPart + file.getName() + "\" target=\"cmd-detail-frame\">" + cmdName + "</a>" + " " + groupSuffix ;
    }
    
    void transformContents() {
        try {
            String contents = FileUtils.readFileToString( file ) ;
            if( contents.contains( "site.css" ) ) return ;
            String newContents = """
                    <!DOCTYPE html>
                    <html lang="en">
                    <head>
                      <meta charset="UTF-8">
                      <title>3D Commands</title>
                      <link rel="stylesheet" href="../site.css">
                    </head>
                    <body>
                    """ + contents + """
                    </body>
                    </html>
                    """;
            
            FileUtils.writeStringToFile( file, newContents ) ;
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }
    
    boolean relinkCommandReferences() throws IOException {
        
        log.debug( "Find linked commands in " + file.getName() ) ;
        Pattern pattern  = Pattern.compile( "<a href=\"([^\"]+?)\" class=\"xref page\">" ) ;
        String  contents = FileUtils.readFileToString( file ) ;
        Matcher matcher  = pattern.matcher( contents ) ;
        boolean cmdFound = false ;
        
        int           lastEnd    = 0 ;
        StringBuilder newContent = new StringBuilder();
        
        while( matcher.find() ) {
            String href = matcher.group(1) ;
            int start = matcher.start(1) ;
            int end = matcher.end(1) ;
            
            if( href.endsWith( "/" ) ) {
                String cmdName = href.substring( 0, href.length() - 1 ) ;
                cmdName = cmdName.substring( cmdName.lastIndexOf( '/' ) + 1 ) ;
                
                if( GeogebraMkSite.CMD_MAP.containsKey( cmdName ) ) {
                    cmdFound = true ;
                    log.debug( "   Found linked command : " + cmdName ) ;
                    Command cmd = GeogebraMkSite.CMD_MAP.get( cmdName ) ;
                    newContent.append( contents, lastEnd, start ) ;
                    newContent.append( "../" + cmd.groupName + "/" + cmdName + ".html" ) ;
                    lastEnd = end ;
                }
            }
        }
        
        if( lastEnd < contents.length() ) {
            newContent.append( contents, lastEnd, contents.length() ) ;
        }
        
        if( cmdFound ) {
            FileUtils.writeStringToFile( file, newContent.toString() ) ;
        }
        return cmdFound ;
    }}
