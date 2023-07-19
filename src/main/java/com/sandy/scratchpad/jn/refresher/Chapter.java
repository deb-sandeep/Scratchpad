package com.sandy.scratchpad.jn.refresher;

import java.io.File ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class Chapter {
    
    private static final Logger log = Logger.getLogger( Chapter.class ) ;

    private Subject subject = null ;
    private String name = null ;
    private File folder = null ;
    
    public Chapter( Subject sub, int chapterNumber, String name ) {
        this.subject = sub ;
        this.name = name.trim() ;
        
        String chapterName = String.format( "%02d - %s", chapterNumber, name ) ;
        log.debug( "Chapter name = " + chapterName ) ; 
        this.folder = new File( this.subject.getFolder(), chapterName ) ;
    }
    
    public String getName() {
        return this.name ;
    }
    
    public File getFolder() {
        return this.folder ;
    }
    
    public void createFolderStructure() throws Exception {
        log.debug( "Creating folder structure for chapter " + getName() ) ;
        this.folder.mkdirs() ;
        createDummyFile( this.folder, "dummy.txt", "Dummy file." ) ;
    }
    
    private void createDummyFile( File folder, String fileName, String text ) 
        throws Exception {
        
        File file = new File( folder, fileName ) ;
        FileUtils.writeStringToFile( file, text ) ;
    }    
}
