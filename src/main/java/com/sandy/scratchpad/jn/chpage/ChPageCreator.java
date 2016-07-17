package com.sandy.scratchpad.jn.chpage;

import java.io.File ;
import java.io.FileFilter ;
import java.io.FileWriter ;
import java.util.Arrays ;
import java.util.Comparator ;

public class ChPageCreator {
    
    private String[] subjects = { "Literature - MoV" } ;
    private File jnRoot = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes/Class-9" ) ;
    
    public void execute() throws Exception {
        for( String subject : subjects ) {
            createPagesForSubject( subject ) ;
        }
    }
    
    private void createPagesForSubject( String subject ) throws Exception {
        File subDir = new File( jnRoot, subject ) ;
        File[] chapters = subDir.listFiles() ;
        
        for( File chapter : chapters ) {
            File hiResImgDir = new File( chapter, "img/pages/hi-res" ) ;
            if( hiResImgDir.exists() ) {
                createPageForChapter( subject, chapter ) ;
            }
        }
    }
    
    private void createPageForChapter( String subject, File chapter ) throws Exception {

        System.out.println( chapter.getAbsolutePath() ) ;
        
        File pagesDir = new File( chapter, "img/pages" ) ;
        File[] pageImgs = pagesDir.listFiles( new FileFilter() {
            public boolean accept( File pathname ) {
                if( pathname.getName().endsWith( ".png" ) ) {
                    return true ;
                }
                return false ;
            }
        } ) ;
        
        Arrays.sort( pageImgs, new Comparator<File>() {
            public int compare( File o1, File o2 ) {
                return (int)( o1.lastModified() - o2.lastModified() ) ;
            }
        } );
        
        writePageChapter( subject, chapter, pageImgs ) ;
    }
    
    private void writePageChapter( String subject, File chapter, File[] pageImgFiles ) 
        throws Exception {
        
        String chapterName = chapter.getName() ;
        String chapterNumStr = chapterName.substring( 0, chapterName.indexOf( "-" ) ).trim() ;
        chapterName = chapterName.substring( chapterName.indexOf( "-" ) + 1 ).trim() ;
        
        int chapterNum    = Integer.parseInt( chapterNumStr ) ;
        
        File textChp = new File( chapter, chapterNum + ".0 - " + chapterName + " (text).jn" ) ;

        FileWriter fw = new FileWriter( textChp ) ;

        fw.write( "@skip_generation_in_production\n" ) ;
        fw.write( "\n" ) ;
        fw.write( "subject \"" + subject + "\"\n" ) ;
        fw.write( "chapterNumber " + chapterNum + ".0\n" ) ;
        fw.write( "chapterName \"" + chapterName + " (text)\"\n" ) ;
        fw.write( "\n" ) ;
        fw.write( "@tn \"Lesson text\"\n" ) ;
        fw.write( "\"{{@carousel\n" ) ;
        for( File imgFile : pageImgFiles ) {
            fw.write( "    @slide pages/" + imgFile.getName() + "\n" ) ;
        }
        fw.write( "}}\"\n" ) ;        
        fw.flush(); 
        fw.close();
    }

    public static void main( String[] args ) throws Exception {
        new ChPageCreator().execute() ;
    }
}
