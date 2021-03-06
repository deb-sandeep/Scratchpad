package com.sandy.scratchpad.jn.exercise;

import java.io.File ;
import java.io.FilenameFilter ;
import java.util.ArrayList ;
import java.util.List ;
import java.util.Map ;
import java.util.Map.Entry ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class ExerciseGen {
    
    private static final Logger log = Logger.getLogger( ExerciseGen.class ) ;
    
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
        File[] potentialChapters = bookFolder.listFiles() ;
        
        if( potentialChapters == null || 
            potentialChapters.length == 0 ) {
            return ;
        }
        
        for( File potentialChapter : potentialChapters ) {
            if( potentialChapter.isDirectory() ) {
                processPotentialBookChapter( bookName, potentialChapter ) ;
            }
        }
    }
    
    private void processPotentialBookChapter( String bookName, File chapterFolder )
        throws Exception {
        
        String chapterName = chapterFolder.getName() ;
        generateExercises( bookName, chapterName, null ) ;
    }
    
    public void generateExercises( String bookName, String chapterName, List<String> exerciseNames ) 
        throws Exception {
        
        
        File imgFolder = null ;
        if( bookName == null ) {
            imgFolder = new File( this.chpFolder, "img" ) ;
        }
        else {
            imgFolder = new File( this.chpFolder, 
                                  "img/books/" + bookName + "/" + chapterName ) ;
        }
        
        File[] relevantFiles = getRelevantImageFiles( imgFolder ) ;
        QuestionManager qMgr = new QuestionManager( bookName, chapterName ) ;
        
        for( File file : relevantFiles ) {
            
            String fileName = file.getName() ;
            qMgr.buildImageMeta( fileName.substring( 0, fileName.length()-4 ) ) ;
        }
        
        Map<String, Exercise> exMap = qMgr.createQuestions() ;
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
                id += " (" + ex.getBookName() + ":" + ex.getChapterName() + ")" ;
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
                    
                    if( name.startsWith( "Ex" ) || 
                        name.startsWith( "ex_" ) ) {
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
        
        int marks = q.isPartOfGroup() ? 50 : 70 ;
        
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
            buffer.append( "Get it checked by teacher" ) ;
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
            return "{{@img " + imgMeta + ".png}}" ;
        }
        else {
            return "{{@img books/" + ex.getBookName() + "/" + 
                                     ex.getChapterName() + "/" + 
                                     imgMeta + ".png}}" ;
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
            buffer.append( " [" + ex.getBookName() + "_" + ex.getChapterName() + "]" ) ;
        }
              
        buffer.append( " (ex-" )
              .append( exName )
              .append( ").jn" ) ;
        
        return new File( this.chpFolder, buffer.toString() ) ;
    }
    
    private String getJNFileHeader( Exercise ex ) {
        
        String exName = ex.getExId() ;
        if( exName.equals( "ex_" ) ) {
            exName = "Examples" ;
        }
        
        String chapterName = baseChpName ;
        
        if( ex.getBookName() != null ) {
            chapterName += " [" + 
                           ex.getBookName() + "_" + ex.getChapterName() + 
                           "]" ;
        }
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( "@exercise_bank\n\n" )
              .append( "subject \"").append( subjectName ).append( "\"\n" )
              .append( "chapterNumber " + chapterNumber + "." + nextSubChapterNumber + "\n" )
              .append( "chapterName \"" + chapterName +  " (ex-" + exName + ")\"\n" )
              .append( "\n" ) ;
              
        return buffer.toString() ;
    }

    public static void main( String[] args ) throws Exception {
        
        String JN_ROOT_DIR      = "/home/sandeep/Documents/StudyNotes/JoveNotes-V" ;
        String JN_CLS_DIR       = "Class-5" ;
        String JN_SUBJECT       = "Mathematics" ;
        String JN_CHAPTER       = "05 - Multiples and Factors" ;
        String JN_BASE_CHP_NAME = "Multiples and Factors" ;
        int    JN_CHAPTER_NUM   = 5 ;
        int    JN_SUB_CHP_START = 2 ;
        String includedExercises[]  = {} ;

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
        gen.generateExercises( null, null, exerciseNames ) ;
        gen.generateExercisesForBooks() ;
    }
}
