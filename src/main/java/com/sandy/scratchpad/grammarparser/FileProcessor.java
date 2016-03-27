package com.sandy.scratchpad.grammarparser;

import java.io.File ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.log4j.Logger ;

class Question {
    
    private static final Logger logger = Logger.getLogger( Question.class ) ;
    
    List<String> qLines = new ArrayList<String>() ;
    List<String> aLines = new ArrayList<String>() ;
    
    public void dump() {
        for( String s : qLines ) {
            logger.debug( s ) ;
        }
        for( String s : aLines ) {
            logger.debug( s ) ;
        }
    }
}

public class FileProcessor {
    
    private static final Logger logger = Logger.getLogger( FileProcessor.class ) ;
    
    private static int STATE_HDR = 0 ;
    private static int STATE_Q   = 1 ;
    private static int STATE_A   = 2 ;
    
    private File         file  = null ;
    private List<String> lines = null ;
    private List<String> hdrLines = new ArrayList<String>() ;
    private List<String> qLines   = new ArrayList<String>() ;
    private List<String> aLines   = new ArrayList<String>() ;
    
    private List<Question> questions = new ArrayList<Question>() ;

    public FileProcessor( File file, List<String> lines ) {
        this.file = file ;
        this.lines = lines ;
        segregateLines() ;
    }
    
    private void segregateLines() {
        
        int state = STATE_HDR ;
        for( String line : lines ) {
            line = line.trim() ;
            line = line.replaceAll( "â¦", "_" ) ;
            line = line.replaceAll( "_\\.\\.", "_" ) ;
            line = line.replaceAll( "_\\.", "_" ) ;
            line = line.replaceAll( "â", "'" ) ;
            if( line.matches( "Answers" ) ) {
                state = STATE_A ;
                continue ;
            }
            else if( line.matches( "^[0-9]*\\..*" ) ) {
                if( state == STATE_HDR ) {
                    state = STATE_Q ;
                }
            }

            if( state == STATE_HDR ) {
                hdrLines.add( line ) ;
            }
            else if( state == STATE_Q ) {
                qLines.add( line ) ;
            }
            else {
                aLines.add( line ) ;
            }
        }
    }
    
    public void process() throws Exception {
        
        Question curQ = null ;
        
        for( String qLine : qLines ) {
            if( qLine.matches( "^[0-9]*\\..*" ) ) {
                if( curQ != null ) {
                    associateAnswer( curQ ) ;
                }
                curQ = new Question() ;
                questions.add( curQ ) ;
            }
            curQ.qLines.add( qLine ) ;
        }
        if( curQ != null ) {
            associateAnswer( curQ ) ;
        }
        
        saveNewFile() ;
    }
    
    private void saveNewFile() throws Exception {
        
        File outputFile = new File( file.getParentFile().getParentFile(), 
                                    "processed/" + file.getName() ) ;
        List<String> contents = new ArrayList<String>() ;
        contents.addAll( hdrLines ) ;
        contents.add( "" ) ;
        for( Question q : questions ) {
            contents.addAll( q.qLines ) ;
            contents.addAll( q.aLines ) ;
            contents.add( "" ) ;
        }
        
        FileUtils.writeLines( outputFile, contents ) ;
    }
    
    private void associateAnswer( Question q ) {

        String firstQLine = q.qLines.get( 0 ) ;
        String qId        = firstQLine.substring( 0, firstQLine.indexOf( "." ) ) ;
        
        boolean collectingAnswer = false ;
        for( String aLine : aLines ) {
            if( aLine.matches( "^[0-9]*\\..*" ) ) {
                if( aLine.startsWith( qId ) ) {
                    collectingAnswer = true ;
                    aLine = "Ans " + aLine ;
                }
                else {
                    if( collectingAnswer == true ) {
                        return ;
                    }
                    else {
                        continue ;
                    }
                }
            }
            
            if( collectingAnswer ) {
                q.aLines.add( aLine ) ;
            }
        }
    }
    
    public void dump() {
        print( "Header", hdrLines ) ;
//        print( "Questions", qLines ) ;
//        print( "Answers", aLines ) ;
        for( Question q : questions ) {
            q.dump() ;
            logger.debug( "" ) ;
        }
    }
    
    private void print( String hdr, List<String> lines ) {
        logger.debug( hdr ) ;
        logger.debug( "---------------------------------------------------------------" ) ; 
        for( String line : lines ) {
            logger.debug( line ) ;
        }
    }
}
