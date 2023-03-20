package com.sandy.scratchpad.jn.exercise;

import java.io.File ;
import java.io.FilenameFilter ;
import java.util.ArrayList ;
import java.util.List ;
import java.util.Map ;
import java.util.Map.Entry ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

public class ExerciseGen {
    
    private static final Logger log = Logger.getLogger( ExerciseGen.class ) ;
    
    public static void main( String[] args ) throws Exception {

        String[] chapterNames = {
            "01 - Language of Chemistry"
        } ;

        for( String name : chapterNames ) {
            generateExercises( name ) ;
        }
    }
    
    public static void generateExercises( String chapterName ) 
        throws Exception {
        
        String JN_ROOT_DIR      = "/Users/sandeep/Documents/StudyNotes/JoveNotes-Std-9/" ;
        String JN_CLS_DIR       = "Class-9" ;
        String JN_SUBJECT       = "Chemistry" ;
        String JN_CHAPTER       = chapterName ;
        String JN_BASE_CHP_NAME = null ;
        int    JN_CHAPTER_NUM   = -1 ;
        int    JN_SUB_CHP_START = 4 ;
        String includedExercises[]  = {} ;
        
        if( JN_BASE_CHP_NAME == null ) {
            JN_BASE_CHP_NAME = getBaseChapterName( JN_CHAPTER ) ;
        }
        
        if( JN_CHAPTER_NUM == -1 ) {
            JN_CHAPTER_NUM = getBaseChapterNum( JN_CHAPTER ) ;
        }
        
        File rootJNDir = new File( JN_ROOT_DIR ) ;
        File clsJNDir  = new File( rootJNDir, JN_CLS_DIR ) ;
        File subJNDir  = new File( clsJNDir,  JN_SUBJECT ) ;
        File chpJNDir  = new File( subJNDir,  JN_CHAPTER ) ;
        
        List<String> exerciseNames = new ArrayList<>() ;
        for( String arg : includedExercises ) {
            exerciseNames.add( arg ) ;
        }
        
        ExerciseGen gen = new ExerciseGen( chpJNDir, 
                                           JN_SUBJECT, 
                                           JN_BASE_CHP_NAME, 
                                           JN_CHAPTER_NUM, 
                                           JN_SUB_CHP_START ) ;
        gen.generateExercises( null, exerciseNames ) ;
        gen.generateExercisesForBooks() ;
    }

    private static String getBaseChapterName( String chapterName ) {
        int index = chapterName.indexOf( '-' ) ;
        if( index == -1 ) {
            throw new IllegalArgumentException( "Chapter name invalid. - not found" ) ;
        }
        return chapterName.substring( index + 2 ).trim() ;
    }
    
    private static int getBaseChapterNum( String chapterName ) {
        int index = chapterName.indexOf( '-' ) ;
        if( index == -1 ) {
            throw new IllegalArgumentException( "Chapter name invalid. - not found" ) ;
        }
        String str = chapterName.substring( 0, index ).trim() ;
        return Integer.parseInt( str ) ;
    } 
    
    // =========================================================================
    
    // jnFolder is the path till the chapter - not the root JN folder!
    private File   chpFolder             = null ;
    private String baseChpName           = null ;
    private int    chapterNumber         = 0 ;
    private int    subChapterStartNumber = 0 ;
    private String subjectName           = null ;
    private int    nextSubChapterNumber  = 0 ;
    
    public ExerciseGen( File chpFolder, 
                        String subjectName,
                        String baseChpName,
                        int chapterNumber, 
                        int subChapterStartNumber ) {

        validateInputFolder( chpFolder ) ;
        
        this.chpFolder = chpFolder ;
        this.subjectName = subjectName ;
        this.baseChpName = baseChpName ;
        this.chapterNumber = chapterNumber ;
        this.subChapterStartNumber = subChapterStartNumber ;
        this.nextSubChapterNumber = this.subChapterStartNumber ;
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
    
    public void generateExercisesForBooks() throws Exception {
        
        File booksFolder = new File( this.chpFolder, "img/books" ) ;
        if( !booksFolder.exists() ) {
            return ;
        }
        
        File[] potentialBookFolders = booksFolder.listFiles() ;
        if( potentialBookFolders == null ||
            potentialBookFolders.length == 0 ) {
            return ;
        }
        
        for( File potentialBookFolder : potentialBookFolders ) {
            if( potentialBookFolder.isDirectory() ) {
                processPotentialBookFolder( potentialBookFolder ) ;
            }
        }
    }
    
    private void processPotentialBookFolder( File bookFolder ) 
        throws Exception {
        
        String bookName = bookFolder.getName() ;
        generateExercises( bookName, null ) ;
    }
    
    public void generateExercises( String bookName, List<String> exerciseNames ) 
        throws Exception {
        
        
        File imgFolder = null ;
        if( bookName == null ) {
            imgFolder = new File( this.chpFolder, "img/exercise" ) ;
        }
        else {
            imgFolder = new File( this.chpFolder, 
                                  "img/books/" + bookName + "/exercise" ) ;
        }
        
        File[] relevantFiles = getRelevantImageFiles( imgFolder ) ;
        QuestionManager qMgr = new QuestionManager( bookName ) ;
        
        for( File file : relevantFiles ) {
            String fileName = file.getName() ;
            qMgr.buildImageMeta( fileName ) ;
        }
        
        qMgr.printImgMetaList() ;
        
        Map<String, Exercise> exMap = qMgr.createExercises() ;
        for( Exercise ex : exMap.values() ) {
            
            String id = ex.getExId() ;
            
            log.debug( "Generating questions for exercise id = " + id );
            
            if( exerciseNames != null && !exerciseNames.isEmpty() ) {
                if( !exerciseNames.contains( id ) ) {
                    log.debug( "\tSkipping exercise as it's not in the CLP list" ) ;
                    continue ;
                }
            }
            
            if( ex.getBookName() != null ) {
                id += " (" + ex.getBookName() + ")" ;
            }
            
            log.debug( "Generating JN for exercise " + id );
            generateJNForExercise( ex ) ;
            nextSubChapterNumber++ ;
        }
    }
    
    private File[] getRelevantImageFiles( File imgDir ) {
        
        File[] relevantFiles = imgDir.listFiles( new FilenameFilter() {
            
            public boolean accept( File dir, String name ) {
                
                File file = new File( dir, name ) ;
                if( !file.isDirectory() ) {
                    
                    if( name.startsWith( "Ch" ) ) {
                        return true ;
                    }
                    else if( name.endsWith( ".png" ) ) {
                        log.info( "Not picking up image file " + name ) ;
                    }
                }
                return false ;
            }
        } ) ; 
        
        return relevantFiles ;
    }
    
    void printExercises( Map<String, Exercise> exMap ) {
        
        for( Entry<String, Exercise> entry : exMap.entrySet() ) {
            log.debug( "===================================================" ) ;
            Exercise ex = entry.getValue() ;
            log.debug( ex.getExId() ) ;
            
            for( Question baseQ : ex.getQuestions() ) {
                if( baseQ instanceof QuestionGroup ) {
                    QuestionGroup group = ( QuestionGroup )baseQ ;
                    for( Question q : group.getQuestions() ) {
                        printQuestion( q ) ;
                    }
                }
                else {
                    printQuestion( baseQ ) ;
                }
            }
        }
    }
    
    void printQuestion( Question q ) {
        
        log.debug( "-------------------------------------------------------" ) ;
        log.debug( "Question: " + q.getId() );
        if( q.isPartOfGroup() ) {
            for( ImgMeta hdr : q.getGroup().getHeaders() ) {
                log.debug( "\t" + hdr ) ;
            }
            log.debug( "" ) ;
        }
        
        for( ImgMeta qPart : q.getQuestionParts() ) {
            log.debug( "\t" + qPart ) ;
        }
        
        log.debug( "........................." ) ;
        for( ImgMeta aPart : q.getAnswerParts() ) {
            log.debug( "\t" + aPart ) ;
        }
    }
    
    private void generateJNForExercise( Exercise ex ) 
        throws Exception {
        
        File   chpFile = getJNExFile( ex ) ;
        String fileHdr = getJNFileHeader( ex ) ;
        
        FileUtils.writeStringToFile( chpFile, fileHdr, false ) ;
        
        List<Question> questions = ex.getQuestionsForPrinting() ;
        for( Question q : questions ) {
            writeQuestionToFile( ex, chpFile, q ) ; 
        }
    }
    
    private void writeQuestionToFile( Exercise ex, File file, Question q ) 
        throws Exception {
        
        int marks = q.isPartOfGroup() ? 20 : 50 ;
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( "// " + q.getId() + "\n" )
              .append( "@exercise marks=" + marks + "\n" )
              .append( "\"" ) ;
        
        if( q.isPartOfGroup() ) {
            List<ImgMeta> hdrs = q.getGroup().getHeaders() ;
            
            for( int i=0; i<hdrs.size(); i++ ) {
                ImgMeta hdr = hdrs.get( i ) ;
                buffer.append( getTaggedImage( ex, hdr ) ) ;
                buffer.append( "  \n" ) ;
            }
            buffer.append( "\n" ) ;
        }
        
        for( int i=0; i<q.getQuestionParts().size(); i++ ) {
            ImgMeta qPart = q.getQuestionParts().get( i ) ;
            buffer.append( getTaggedImage( ex, qPart ) ) ;
            if( i < q.getQuestionParts().size()-1 ) {
                buffer.append( "  \n" ) ;
            }
        }
        buffer.append( "\"\n" ) ;
        buffer.append( "answer\n\"" ) ;
        
        if( q.getAnswerParts().size() == 0 ) {
            
            String bookStr = ex.getBookName() ;
            if( StringUtil.isEmptyOrNull( bookStr ) ) {
                bookStr = "" ;
            }
            else {
                bookStr = "[Book: " + bookStr + "] " ;
            }
            
            buffer.append( "Check answer " + 
                           bookStr + 
                           "Chapter: " + this.chapterNumber + ", " + 
                           "Problem: " + 
                           q.getExerciseName() + "-" + q.getId() ) ;
        }
        else {
            for( int i=0; i<q.getAnswerParts().size(); i++ ) {
                ImgMeta aPart = q.getAnswerParts().get( i ) ;
                buffer.append( getTaggedImage( ex, aPart ) ) ;
                if( i < q.getAnswerParts().size()-1 ) {
                    buffer.append( "  \n" ) ;
                }
            }
        }
        buffer.append( "\"\n\n" ) ;
        
        FileUtils.writeStringToFile( file, buffer.toString(), true ) ;
    }
    
    private String getTaggedImage( Exercise ex, ImgMeta imgMeta ) {
        
        if( ex.getBookName() == null ) {
            return "{{@img exercise/" + imgMeta + "}}" ;
        }
        else {
            return "{{@img books/" + ex.getBookName() + "/exercise/" + 
                                     imgMeta + "}}" ;
        }
    }
    
    private File getJNExFile( Exercise ex ) {
        
        String exName = ex.getExId() ;
        if( exName.equals( "ex_" ) ) {
            exName = "Examples" ;
        }
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( this.chapterNumber + "." + nextSubChapterNumber )
              .append( " - " )
              .append( this.baseChpName ) ;
        
        if( ex.getBookName() != null ) {
            buffer.append( " [" + ex.getBookName() + "]" ) ;
        }
              
        buffer.append( " (" )
              .append( exName )
              .append( ").jn" ) ;
        
        return new File( this.chpFolder, buffer.toString() ) ;
    }
    
    private String getJNFileHeader( Exercise ex ) {
        
        String exName = "Ex" + ex.getExId() ;
        
        String chapterName = baseChpName ;
        
        if( ex.getBookName() != null ) {
            chapterName += " [" + ex.getBookName() + "]" ;
        }
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( "@exercise_bank\n\n" )
              .append( "subject \"").append( subjectName ).append( "\"\n" )
              .append( "chapterNumber " + chapterNumber + "." + nextSubChapterNumber + "\n" )
              .append( "chapterName \"" + chapterName +  " (" + exName + ")\"\n" )
              .append( "\n" ) ;
              
        return buffer.toString() ;
    }
}
