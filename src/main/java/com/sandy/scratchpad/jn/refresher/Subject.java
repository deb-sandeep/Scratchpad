package com.sandy.scratchpad.jn.refresher;

import java.io.File ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.log4j.Logger ;

public class Subject {
    
    private static final Logger log = Logger.getLogger( Subject.class ) ;

    private String name = null ;
    private List<Chapter> chapters = new ArrayList<>() ;
    private File subjectRoot = null ;
    
    public Subject( String name, File jnRoot ) {
        this.name = name ;
        this.subjectRoot = new File( jnRoot, name ) ;
    }
    
    public void addChapter( Chapter chapter ) {
        chapters.add( chapter ) ;
    }
    
    public void createFolderStructure() throws Exception {
        log.debug( "Creating folder structure for subject " + getName() ) ;
        this.subjectRoot.mkdirs() ;
        
        for( Chapter chapter : chapters ) {
            chapter.createFolderStructure() ;
        }
    }
    
    public String getName() {
        return this.name ;
    }
    
    public File getFolder() {
        return this.subjectRoot ;
    }
}
