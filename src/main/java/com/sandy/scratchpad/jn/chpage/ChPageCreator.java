package com.sandy.scratchpad.jn.chpage;

import java.io.File ;
import java.io.FileFilter ;
import java.io.FileWriter ;
import java.util.Arrays ;
import java.util.Comparator ;

public class ChPageCreator {
    
    private File jnRoot = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes-Std-8/Class-8" ) ;

    private String[] subjects = { 
            "Biology",
            "Chemistry",
            "Civics",
            "English",
            "English Grammar",
            "Geography",
            "Hindi",
            "History",
            "Mathematics",
            "Physics",
            "Rapid Reader"
    } ;
    
    public void execute() throws Exception {
        for( String subject : subjects ) {
            createPagesForSubject( subject ) ;
        }
    }
    
    private void createPagesForSubject( String subject ) throws Exception {
        File subDir = new File( jnRoot, subject ) ;
        File[] chapters = subDir.listFiles() ;
        
        for( File chapter : chapters ) {
            File hiResImgDir = new File( chapter, "img/pages" ) ;
            if( hiResImgDir.exists() ) {
                createPageForChapter( subject, chapter ) ;
            }
        }
    }
    
    public void createPageForChapter( String subject, File chapter ) throws Exception {

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
                int page1No = getPageNum( o1 ) ;
                int page2No = getPageNum( o2 ) ;
                
                return page1No - page2No ;
            }
        } );
        
        writePageChapter( subject, chapter, pageImgs ) ;
    }
    
    private int getPageNum( File f ) {
        String fName = f.getName() ;
        fName = fName.substring( 0, fName.length()-4 ) ;
        String numStr = fName.substring( fName.lastIndexOf( "_" )+1 ) ;
        return Integer.parseInt( numStr ) ;
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
