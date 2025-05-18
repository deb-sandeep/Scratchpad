package com.sandy.scratchpad.geogebra.cmdleech;

import lombok.Data;
import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
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
    
    @Override
    public int hashCode() {
        return this.cmdName.hashCode() ;
    }
    
    @Override
    public boolean equals( Object obj ) {
        if( !(obj instanceof Command) ) {return false;}
        return ((Command)obj).cmdName.equals( this.cmdName ) ;
    }
}
