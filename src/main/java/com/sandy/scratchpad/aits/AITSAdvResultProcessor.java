package com.sandy.scratchpad.aits;

import java.io.File ;
import java.io.FileFilter ;
import java.util.ArrayList ;
import java.util.Collections ;
import java.util.HashSet ;
import java.util.List ;
import java.util.Set ;

import org.apache.log4j.Logger ;

class Question implements Comparable<Question>{
    String subject = null ;
    String paperId = null ;
    String qType = null ;
    int qNo = 0 ;
    int lctNo = 0 ;
    String qSuffix = null ;
    
    Question( String input ) {
        String[] parts = input.split( "_" ) ;
        subject = parts[0] ;
        paperId = parts[2] + "_" + parts[3] ;
        qType   = parts[4] ;
        qSuffix = qType ;
        
        if( qType.equals( "LCT" ) ) {
            lctNo = Integer.parseInt( parts[5] ) ;
            qNo = Integer.parseInt( parts[6] ) ;
            
            qSuffix += lctNo + "_" ;
        }
        else {
            qNo = Integer.parseInt( parts[5] ) ;
        }
        qSuffix += qNo ;
    }
    
    public String toString() {
        String str = subject + "_Q_" + paperId + "_" + qType + "_" ;
        if( lctNo != 0 ) {
            str += lctNo + "_" ;
        }
        str += qNo ;
        return str ;
    }

    @Override
    public int compareTo( Question q ) {
        
        if( qNo == q.qNo ) {
            if( qType.equals( q.qType ) ) {
                if( qType.equals( "LCT" ) ) {
                    return lctNo - q.lctNo ;
                }
            }
            else {
                return qType.compareTo( q.qType ) ;
            }
        }
        return (qNo - q.qNo) ;
    }
}

public class AITSAdvResultProcessor {
    
    private static final Logger log = Logger.getLogger( AITSAdvResultProcessor.class ) ;
    
    private static final File BASE_DIR = new File( "/home/sandeep/projects/source/SConsoleProcessedImages" ) ;
    
    private static final String[] PAPERS = {
        "FJ14_CRS1A2", "FJ14_CRS2A1", "FJ14_CRS2A2",  "FJ14_CRS3A1",  "FJ14_CRS3A2",  "FJ14_CRS4A1",
        "FJ14_CRS4A2", "FJ14_FTS1A1", "FJ14_FTS1A2",  "FJ14_FTS2A1",  "FJ14_FTS2A2",  "FJ14_FTS4A1",
        "FJ14_FTS4A2", "FJ14_FTS5A1", "FJ14_FTS5A2",  "FJ17_CRS1A1",  "FJ17_CRS1A2",  "FJ17_CRS2A1",
        "FJ17_CRS2A2", "FJ17_CRS3A1", "FJ17_CRS3A2",  "FJ17_CRS4A1",  "FJ17_FTS1A1",  "FJ17_FTS1A2",
        "FJ17_FTS2A1", "FJ17_FTS2A2", "FJ17_FTS4A1",  "FJ17_FTS4A2",  "FJ17_FTS5A1",  "FJ17_FTS5A2",
        "FJ18_CRS4A1", "FJ18_CRS4A2", "FJ18_FTS10A1", "FJ18_FTS10A2", "FJ18_FTS11A1", "FJ18_FTS11A2",
        "FJ18_FTS1A1", "FJ18_FTS1A2", "FJ18_FTS2A1",  "FJ18_FTS2A2",  "FJ18_FTS3A1",  "FJ18_FTS3A2",
        "FJ18_FTS4A1", "FJ18_FTS4A2", "FJ18_FTS5A1",  "FJ18_FTS5A2",  "FJ18_FTS6A1",  "FJ18_FTS6A2",
        "FJ18_FTS8A1", "FJ18_FTS8A2", "FJ18_FTS9A1",  "FJ18_FTS9A2",  "FJ18_OTA1",    "FJ18_OTA2",
        "FJ18_PTS1A1", "FJ18_PTS1A2", "FJ18_PTS2A1",  "FJ18_PTS2A2",  "FJ18_PTS3A1",  "FJ18_PTS3A2"
    } ;
    
    private AnswerManager ansManager = null ;
    
    private void execute() throws Exception {
        
        ansManager = new AnswerManager() ;
        ansManager.loadAnswers( new File( BASE_DIR, "AITSAdvSols.txt" ) ) ;
        
        log.debug( "-------------------------------" ) ;
        
        for( String paperId : PAPERS ) {
            processQuestionPaper( paperId ) ;
        }
    }
    
    private void processQuestionPaper( String paperId ) {
        
        List<Question> phyQuestions  = getQuestionsFor( paperId, "IIT - Physics",   "Phy"  ) ;
        List<Question> chemQuestions = getQuestionsFor( paperId, "IIT - Chemistry", "Chem" ) ;
        List<Question> mathQuestions = getQuestionsFor( paperId, "IIT - Maths",     "Math" ) ;
        
        if( !validateQuestionsForPaper( phyQuestions, chemQuestions, mathQuestions ) ) {
            print( "Physics "   + paperId, phyQuestions  ) ;
            print( "Chemistry " + paperId, chemQuestions ) ;
            print( "Maths "     + paperId, mathQuestions ) ;
            System.exit( -1 ) ;
        }
        
        List<Question> allQuestions = new ArrayList<>() ;
        allQuestions.addAll( phyQuestions ) ;
        allQuestions.addAll( chemQuestions ) ;
        allQuestions.addAll( mathQuestions ) ;
        
        lookupAndPrintAnswers( paperId, allQuestions ) ;
    }
    
    private void lookupAndPrintAnswers( String paperId, List<Question> questions ) {

        for( Question question : questions ) {
            String ans = ansManager.lookupAnswer( question ) ;
            
            if( question.qType.equals( "MCA" ) ) {
                String[] parts = ans.split( "" ) ;
                ans = String.join( ",", parts ) ;
            }
            log.debug( question + "=" + ans ) ;
        }
    }
    
    private boolean validateQuestionsForPaper( List<Question> phyQs,
                                               List<Question> chemQs, 
                                               List<Question> mathQs ) {
        if( ( phyQs.size() != chemQs.size() ) ||
            ( phyQs.size() != mathQs.size() ) ) {
            
            log.error( "Size mismatch" ) ;
            return false ;
        }
        
        Set<String> tupuleCheckSet = new HashSet<>() ;
        for( int i=0; i<phyQs.size(); i++ ) {
            tupuleCheckSet.clear() ;
            tupuleCheckSet.add( phyQs.get( i ).qSuffix ) ;
            tupuleCheckSet.add( chemQs.get( i ).qSuffix ) ;
            tupuleCheckSet.add( mathQs.get( i ).qSuffix ) ;
            
            if( tupuleCheckSet.size() != 1 ) {
                log.error( "Question mismatch at index " + (i+1) ) ;
                return false ;
            }
        }
    
        return true ;
    }

    private void print( String header, List<Question> questions ) {
        log.debug( header ) ;
        int i = 1 ;
        for( Question question : questions ) {
            log.debug( "\t[" + i++ + "] " + question ) ;
        }
    }
     
    private List<Question> getQuestionsFor( String paperId, String subject, String filePrefix ) {
        
        List<Question> questionList = new ArrayList<>() ;
        
        File dir = new File( BASE_DIR, subject ) ;
        File[] images = dir.listFiles( new FileFilter() {
            
            public boolean accept( File file ) {
                
                if( !file.isDirectory() ) {
                    String validPrefix = filePrefix + "_Q_" + paperId + "_" ;
                    String fileName = file.getName() ;
                    
                    if( fileName.startsWith( validPrefix ) && 
                        fileName.endsWith( ".png" ) ) {
                        return true ;
                    }
                }
                return false ;
            }
        } ) ;
        
        ArrayList<String> partNames = new ArrayList<>() ;
        
        for( File image : images ) {
            String name = image.getName() ;
            String trimmedName = name.substring( 0, name.length()-4 ) ;
            
            if( trimmedName.contains( "_LCT" ) ) {
                String parts[] = trimmedName.split( "_" ) ;
                if( parts.length == 6 ) {
                    continue ;
                }
            }
            
            if( trimmedName.endsWith( ")" ) ) {
                trimmedName = trimmedName.substring( 0, trimmedName.length()-3 ) ;
                if( partNames.contains( trimmedName ) ) {
                    continue ;
                }
                else {
                    partNames.add( trimmedName ) ;
                }
            }
            
            try {
                questionList.add( new Question( trimmedName ) ) ;
            }
            catch( Exception e ) {
                log.debug( "Check question format - " + trimmedName, e );
            }
        }
        
        Collections.sort( questionList ) ;
        
        return questionList ;
    }
    
    public static void main( String[] args ) throws Exception {
        new AITSAdvResultProcessor().execute() ;
    }
}
