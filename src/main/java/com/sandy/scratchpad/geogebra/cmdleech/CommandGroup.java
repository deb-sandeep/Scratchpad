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
}
