package com.sandy.scratchpad.geogebra.cmdleech;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Cmd {
    
    private static final Logger log = Logger.getLogger( CmdGroup.class ) ;

    private final File file ;
    final String cmdName ;
    final String groupName ;
    
    Cmd( String groupName, File cmdFile ) throws Exception {
        
        String fileName = cmdFile.getName() ;
        
        this.file = cmdFile ;
        this.groupName = groupName ;
        this.cmdName = fileName.substring( 0, fileName.lastIndexOf(".") ) ;
        log.debug( "   Parsing cmd : " + this.cmdName ) ;
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
        Pattern pattern = Pattern.compile( "<a href=\"([^\"]+?)\" class=\"xref page\">" ) ;
        String  contents = FileUtils.readFileToString( file ) ;
        Matcher matcher = pattern.matcher( contents ) ;
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
                    Cmd cmd = GeogebraMkSite.CMD_MAP.get( cmdName ) ;
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
    }
}

class CmdGroup {
    
    private static final Logger log = Logger.getLogger( CmdGroup.class );
    
    private final File           dir;
    private final String         groupName;
    private final ArrayList<Cmd> cmds = new ArrayList<Cmd>();
    
    CmdGroup( File dir ) throws Exception {
        this.dir = dir;
        this.groupName = dir.getName();
        init();
    }
    
    private void init() throws Exception {
        File[] files = dir.listFiles();
        for( File file : files ) {
            if( !file.isDirectory() && !file.getName().equals( "_index.html" ) ) {
                Cmd cmd = new Cmd( groupName, file ) ;
                GeogebraMkSite.CMD_MAP.put( cmd.cmdName, cmd ) ;
                cmds.add( cmd );
            }
        }
    }
    
    List<Cmd> getCmds() {
        return cmds;
    }
    
    void generateIndexFile() {
        cmds.sort( (c1, c2) -> c1.cmdName.compareTo( c2.cmdName ) ) ;
        
        File indexFile = new File( dir, "_index.html" );
        StringBuilder content = new StringBuilder( """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>3D Commands</title>
                  <link rel="stylesheet" href="../style.css">
                </head>
                <body>
                """ ) ;
        content.append( "<h3>" + groupName + "</h3>" ) ;
        content.append( "<ul>" ) ;
        for( Cmd cmd : cmds ) {
            content.append( "<li>" ).append( cmd.getAnchorTag( false ) ).append( "</li>\n" );
        }
        content.append( """
                </ul>
                </body>
                </html>
                """ );
        
        try {
            log.debug( "Writing index file : " + indexFile.getAbsolutePath() ) ;
            FileUtils.writeStringToFile( indexFile, content.toString(), "UTF-8" ) ;
        }
        catch( IOException e ) {
            throw new RuntimeException( e ) ;
        }
    }
}

public class GeogebraMkSite {
    
    private static final Logger log = Logger.getLogger( GeogebraMkSite.class ) ;
    
    public static void main( String[] args ) throws Exception {
        GeogebraMkSite app = new GeogebraMkSite() ;
        app.generateSite() ;
    }
    
    private final ArrayList<CmdGroup> cmdGroups = new ArrayList<>() ;
    private final ArrayList<Cmd> allCmds = new ArrayList<>() ;
    
    public static Map<String, Cmd> CMD_MAP = new HashMap<>() ;
    
    private void generateSite() throws Exception {
        parseContent() ;
        //generateAllCmdsIndex() ;
        //cmdGroups.forEach( CmdGroup::generateIndexFile ) ;
        //allCmds.forEach( Cmd::transformContents ) ;
        for( Cmd cmd : allCmds ) {
            cmd.relinkCommandReferences() ;
        }
    }
    
    private void parseContent() throws Exception {
        File[] files = GeogebraCommandLeecher.BASE_DIR.listFiles() ;
        for ( File file : files ) {
            if ( file.isDirectory() ) {
                log.debug( "Parsing command group : " + file.getName() ) ;
                CmdGroup cmdGroup = new CmdGroup( file ) ;
                allCmds.addAll( cmdGroup.getCmds() ) ;
                cmdGroups.add( cmdGroup ) ;
            }
        }
    }
    
    private void generateAllCmdsIndex() throws Exception {
        
        allCmds.sort( (c1, c2) -> c1.cmdName.compareTo( c2.cmdName ) ) ;
        
        File indexFile = new File( GeogebraCommandLeecher.BASE_DIR, "all-commands.html" );
        StringBuilder content = new StringBuilder( """
                <!DOCTYPE html>
                <html lang="en">
                <head>
                  <meta charset="UTF-8">
                  <title>3D Commands</title>
                  <link rel="stylesheet" href="style.css">
                </head>
                <body>
                """ ) ;
        content.append( "<h3>All Commands</h3>" ) ;
        content.append( "<ul>" ) ;
        for( Cmd cmd : allCmds ) {
            content.append( "<li>" ).append( cmd.getAnchorTag( true ) ).append( "</li>\n" );
        }
        content.append( """
                </ul>
                </body>
                </html>
                """ );
        
        try {
            log.debug( "Writing index file : " + indexFile.getAbsolutePath() ) ;
            FileUtils.writeStringToFile( indexFile, content.toString(), "UTF-8" ) ;
        }
        catch( IOException e ) {
            throw new RuntimeException( e ) ;
        }
    }
}
