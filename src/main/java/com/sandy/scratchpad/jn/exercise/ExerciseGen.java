package com.sandy.scratchpad.jn.exercise;

import java.io.File ;
import java.io.FilenameFilter ;
import java.util.List ;
import java.util.Map ;
import java.util.Map.Entry ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

public class ExerciseGen {
    
    private static final Logger log = Logger.getLogger( ExerciseGen.class ) ;
    
    // jnFolder is the path till the chapter - not the root JN folder!
    private File chpFolder = null ;
    private String baseChpName = null ;
    private int  chapterNumber = 0 ;
    private int  subChapterStartNumber = 0 ;
    private String subjectName = null ;
    
    private QuestionManager qMgr = new QuestionManager() ;
    
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
    
    public void generateExercises() throws Exception {
        
        File[] relevantFiles = getRelevantImageFiles() ;
        for( File file : relevantFiles ) {
            
            String fileName = file.getName() ;
            qMgr.buildImageMeta( fileName.substring( 0, fileName.length()-4 ) ) ;
        }
        
        Map<String, Exercise> exMap = qMgr.createQuestions() ;
        int subChpNum = this.subChapterStartNumber ;
        for( Exercise ex : exMap.values() ) {
            
            log.debug( "Generating JN for exercise " + ex.getName() );
            generateJNForExercise( ex, subChpNum ) ;
            subChpNum++ ;
        }
    }
    
    private File[] getRelevantImageFiles() {
        
        File imgDir = new File( this.chpFolder, "img" ) ;
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
            log.debug( ex.getName() ) ;
            
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
    
    private void generateJNForExercise( Exercise ex, int subChpNum ) 
        throws Exception {
        
        File   chpFile = getJNExFile( ex, subChpNum ) ;
        String fileHdr = getJNFileHeader( ex, subChpNum ) ;
        
        FileUtils.writeStringToFile( chpFile, fileHdr, false ) ;
        
        List<Question> questions = ex.getQuestionsForPrinting() ;
        for( Question q : questions ) {
            writeQuestionToFile( chpFile, q ) ; 
        }
    }
    
    private void writeQuestionToFile( File file, Question q ) 
        throws Exception {
        
        int marks = q.isPartOfGroup() ? 75 : 100 ;
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( "// " + q.getId() + "\n" )
              .append( "@exercise marks=" + marks + "\n" )
              .append( "\"" ) ;
        
        if( q.isPartOfGroup() ) {
            List<ImgMeta> hdrs = q.getGroup().getHeaders() ;
            
            for( int i=0; i<hdrs.size(); i++ ) {
                ImgMeta hdr = hdrs.get( i ) ;
                buffer.append( "{{@img " + hdr + ".png}}" ) ;
                buffer.append( "  \n" ) ;
            }
            buffer.append( "\n" ) ;
        }
        
        for( int i=0; i<q.getQuestionParts().size(); i++ ) {
            ImgMeta qPart = q.getQuestionParts().get( i ) ;
            buffer.append( "{{@img " + qPart + ".png}}" ) ;
            if( i < q.getQuestionParts().size()-1 ) {
                buffer.append( "  \n" ) ;
            }
        }
        buffer.append( "\"\n" ) ;
        buffer.append( "answer\n\"" ) ;
        
        if( q.getAnswerParts().size() == 0 ) {
            buffer.append( "TODO" ) ;
        }
        else {
            for( int i=0; i<q.getAnswerParts().size(); i++ ) {
                ImgMeta aPart = q.getAnswerParts().get( i ) ;
                buffer.append( "{{@img " + aPart + ".png}}" ) ;
                if( i < q.getAnswerParts().size()-1 ) {
                    buffer.append( "  \n" ) ;
                }
            }
        }
        buffer.append( "\"\n\n" ) ;
        
        FileUtils.writeStringToFile( file, buffer.toString(), true ) ;
    }
    
    private File getJNExFile( Exercise ex, int subChpNum ) {
        
        String exName = ex.getName() ;
        if( exName.equals( "ex_" ) ) {
            exName = "Examples" ;
        }
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( this.chapterNumber + "." + subChpNum ).append( " - " )
              .append( this.baseChpName )
              .append( " (ex-" )
              .append( exName )
              .append( ").jn" ) ;
        
        return new File( this.chpFolder, buffer.toString() ) ;
    }
    
    private String getJNFileHeader( Exercise ex, int subChpNum ) {
        
        String exName = ex.getName() ;
        if( exName.equals( "ex_" ) ) {
            exName = "Examples" ;
        }
        
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( "@exercise_bank\n\n" )
              .append( "subject \"").append( subjectName ).append( "\"\n" )
              .append( "chapterNumber " + chapterNumber + "." + subChpNum + "\n" )
              .append( "chapterName \"" + baseChpName +  " (ex-" + exName + ")\"\n" )
              .append( "\n\n" ) ;
              
        return buffer.toString() ;
    }

    public static void main( String[] args ) throws Exception {
        
        String JN_ROOT_DIR      = "/home/sandeep/Documents/StudyNotes/JoveNotes-X" ;
        String JN_CLS_DIR       = "Class-10" ;
        String JN_SUBJECT       = "Physics" ;
        String JN_CHAPTER       = "08 - Current Electricity" ;
        String JN_BASE_CHP_NAME = "Current Electricity" ;
        int    JN_CHAPTER_NUM   = 8 ;
        int    JN_SUB_CHP_START = 2 ;

        File rootJNDir = new File( JN_ROOT_DIR ) ;
        File clsJNDir  = new File( rootJNDir, JN_CLS_DIR ) ;
        File subJNDir  = new File( clsJNDir,  JN_SUBJECT ) ;
        File chpJNDir  = new File( subJNDir,  JN_CHAPTER ) ;
        
        ExerciseGen gen = new ExerciseGen( chpJNDir, 
                                           JN_SUBJECT, 
                                           JN_BASE_CHP_NAME, 
                                           JN_CHAPTER_NUM, 
                                           JN_SUB_CHP_START ) ;
        gen.generateExercises() ;
    }
}
