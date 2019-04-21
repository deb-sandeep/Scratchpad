package com.sandy.scratchpad.jn.syllabus;

import java.io.File ;
import java.io.FileWriter ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.scratchpad.jn.chpage.ChPageCreator ;

public class Chapter {

    private static final Logger log = Logger.getLogger( Chapter.class ) ;

    private Subject subject = null ;
    private String name = null ;
    private File folder = null ;
    private int chapterNumber = 0 ;
    
    public Chapter( Subject sub, int chapterNumber, String name ) {
        this.subject = sub ;
        this.name = name.trim() ;
        this.chapterNumber = chapterNumber ;
        
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
        
        createDocFolder() ;
        createImgFolder() ;
        createJNQAFile() ;
    }
    
    private void createDocFolder() throws Exception {
        
        File docFolder = new File( this.folder, "doc" ) ;
        docFolder.mkdirs() ;
        createDummyFile( docFolder, "ocr.txt", this.folder.getName() + "\n=============================================\n\n" ) ;
    }
    
    private void createImgFolder() throws Exception {
        
        File imgFolder = new File( this.folder, "img/pages/hi-res" ) ;
        imgFolder.mkdirs() ;
        createDummyFile( imgFolder, "dummy.txt", "" ) ;
    }
    
    private void createDummyFile( File folder, String fileName, String text ) 
        throws Exception {
        
        File file = new File( folder, fileName ) ;
        FileUtils.writeStringToFile( file, text ) ;
    }

    private void createJNQAFile() 
        throws Exception {
        
        File textChp = new File( this.folder, chapterNumber + ".1 - " + name + " (qa).jn" ) ;

        FileWriter fw = new FileWriter( textChp ) ;

        fw.write( "@skip_generation_in_production\n" ) ;
        fw.write( "\n" ) ;
        fw.write( "subject \"" + subject.getName() + "\"\n" ) ;
        fw.write( "chapterNumber " + chapterNumber + ".1\n" ) ;
        fw.write( "chapterName \"" + name + " (qa)\"\n" ) ;
        fw.write( "\n" ) ;
        fw.write( "\n" ) ;
        fw.flush(); 
        fw.close();
    }
}
