package com.sandy.scratchpad.jn.exercise;

import java.io.File ;
import java.io.FilenameFilter ;

import org.apache.log4j.Logger ;

public class ExerciseGen {
    
    private static final Logger log = Logger.getLogger( ExerciseGen.class ) ;
    
    // jnFolder is the path till the chapter - not the root JN folder!
    private File chpFolder = null ;
    private int  chapterNumber = 0 ;
    private int  subChapterStartNumber = 0 ;
    
    private QuestionManager qMgr = new QuestionManager() ;
    
    public ExerciseGen( File chpFolder, 
                        int chapterNumber, 
                        int subChapterStartNumber ) {

        validateInputFolder( chpFolder ) ;
        
        this.chpFolder = chpFolder ;
        this.chapterNumber = chapterNumber ;
        this.subChapterStartNumber = subChapterStartNumber ;
    }
    
    private void validateInputFolder( File dir ) {
        
        if( !dir.exists() ) {
            throw new IllegalArgumentException( "Folder " + dir.getAbsolutePath() + 
                                                " does not exist." ) ;
        }
        else {
            File imgDir = new File( dir, "img" ) ;
            if( !imgDir.exists() ) {
                throw new IllegalArgumentException( "Image folder " + 
                                 imgDir.getAbsolutePath() + "does not exist" ) ;
            }
        }
    }
    
    public void generateExercises() {
        
        File[] relevantFiles = getRelevantImageFiles() ;
        for( File file : relevantFiles ) {
            
            String fileName = file.getName() ;
            qMgr.buildImageMeta( fileName.substring( 0, fileName.length()-4 ) ) ;
        }
        
        qMgr.createQuestions() ;
    }
    
    private File[] getRelevantImageFiles() {
        
        File imgDir = new File( this.chpFolder, "img" ) ;
        File[] relevantFiles = imgDir.listFiles( new FilenameFilter() {
            
            public boolean accept( File dir, String name ) {
                
                File file = new File( dir, name ) ;
                if( !file.isDirectory() ) {
                    
                    if( name.startsWith( "Ex" ) || 
                        name.startsWith( "ex_" ) ||
                        name.endsWith( ".png" ) ) {
                        return true ;
                    }
                }
                return false ;
            }
        } ) ; 
        
        return relevantFiles ;
    }
    
    

    public static void main( String[] args ) {

        File rootJNDir = new File( "/home/sandeep/Documents/StudyNotes/JoveNotes/" ) ;
        File clsJNDir  = new File( rootJNDir, "Class-10" ) ;
//        File clsJNDir  = new File( rootJNDir, "Class-9" ) ;
        File subJNDir  = new File( clsJNDir, "Mathematics" ) ;
        File chpJNDir  = new File( subJNDir, "06 - Quadratic Equations" ) ;
//        File chpJNDir  = new File( subJNDir, "10 - Midpoint Theorem" ) ;
        
        ExerciseGen gen = new ExerciseGen( chpJNDir, 10, 1 ) ;
        gen.generateExercises() ;
    }
}
