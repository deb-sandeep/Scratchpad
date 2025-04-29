package com.sandy.scratchpad.geogebra.cmdleech;

import lombok.Data;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Data
public class CommandGroup {
    
    private static final Logger log = Logger.getLogger( CommandGroup.class );
    
    private final File dir;
    private final String groupName;
    private final ArrayList<Command> commands = new ArrayList<>();
    
    CommandGroup( File dir ) throws Exception {
        this.dir = dir;
        this.groupName = dir.getName();
        init();
    }
    
    private void init() throws Exception {
        File[] files = dir.listFiles();
        assert files != null;
        for( File file : files ) {
            if( !file.isDirectory() &&
                !file.getName().equals( "_index.html" ) &&
                !file.getName().equals( ".DS_Store" )) {
                
                Command cmd = new Command( groupName, file ) ;
                GeogebraMkSite.CMD_MAP.put( cmd.cmdName, cmd ) ;
                commands.add( cmd );
            }
        }
    }
    
    List<Command> getCommands() {
        return commands;
    }
    
    void generateIndexFile() {
        commands.sort( ( c1, c2) -> c1.cmdName.compareTo( c2.cmdName ) ) ;
        
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
        for( Command cmd : commands ) {
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
