package com.sandy.scratchpad.jn.englang.scrape;

import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.lang.WordUtils ;
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

public class ExerciseContentProcessor {
    
    private static final Logger logger = Logger.getLogger( ExerciseContentProcessor.class ) ;
    
    private static int STATE_HDR = 0 ;
    private static int STATE_Q   = 1 ;
    private static int STATE_A   = 2 ;
    
    private List<String> lines = null ;
    private List<String> hdrLines = new ArrayList<String>() ;
    private List<String> qLines   = new ArrayList<String>() ;
    private List<String> aLines   = new ArrayList<String>() ;
    
    private List<Question> questions = new ArrayList<Question>() ;
    private ExerciseMetaData meta = null ;
    
    public ExerciseContentProcessor( ExerciseMetaData meta, List<String> lines ) {
        this.meta = meta ;
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
            line = line.replaceAll( "…+(\\s*\\.)+", "________" ) ;
            
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
            else if( state == STATE_A ){
                aLines.add( line ) ;
            }
            else {
                throw new RuntimeException( "FOund an illeagel state" ) ;
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
        outputProcessingResult() ;
    }
    
    private void associateAnswer( Question q ) {

        String firstQLine = q.qLines.get( 0 ) ;
        String qId        = firstQLine.substring( 0, firstQLine.indexOf( "." ) ) ;
        
        boolean collectingAnswer = false ;
        for( String aLine : aLines ) {
            if( aLine.matches( "^[0-9]*\\..*" ) ) {
                if( aLine.startsWith( qId ) ) {
                    collectingAnswer = true ;
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
    
    public String getQuestionBank() {
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( "// Building question bank from :\n" )
              .append( "// Subject : " + meta.getDescription() + "\n" )
              .append( "// URL : " + meta.getUrl().toString() + "\n" )
              .append( "// Publish date : " + GrammarExerciseTOC.SDF.format( meta.getPublishDate() ) + "\n" )
              .append( "\n" ) ;
        
        for( Question q : questions ) {
            String question = buildQuestion( q ) ;
            buffer.append( question ) ;
            buffer.append( "\n\n" ) ;
        }
        return buffer.toString() ;
    }
    
    private String buildQuestion( Question q ) {
        
        StringBuilder aBuffer = new StringBuilder() ;
        StringBuilder qBuffer = new StringBuilder() ;

        qBuffer.append( "@qa " )
               .append( "\"" ) ;
        for( int i=0; i<hdrLines.size(); i++ ) {
            String hdrLine = hdrLines.get( i ) ;
            qBuffer.append( WordUtils.wrap( hdrLine, 80 ) ) ;
            qBuffer.append( "\n\n" ) ;
        }
        
        qBuffer.append( "--------------------------------\n\n" ) ;
        
        for( int i=0; i<q.qLines.size(); i++ ) {
            String qLine = q.qLines.get( i ) ;
            qLine = qLine.replaceFirst( "^[0-9]*\\. ", "" ) ;
            qBuffer.append( WordUtils.wrap( qLine, 80 ) ) ;
            
            if( i < q.qLines.size()-1 ) {
                qBuffer.append( "\n\n" ) ;
            }
        }
        qBuffer.append( "\"" ) ;
        
        aBuffer.append( "\"" ) ;
        for( int i=0; i<q.aLines.size(); i++ ) {
            String aLine = q.aLines.get( i ) ;
            aLine = aLine.replaceFirst( "^[0-9]*\\. ", "" ) ;
            aBuffer.append( WordUtils.wrap( aLine, 80 ) ) ;
            
            if( i < q.aLines.size()-1 ) {
                aBuffer.append( "\n\n" ) ;
            }
        }
        aBuffer.append( "\"" ) ;
        
        String jnText = qBuffer.toString() ;
        jnText += "\n" ;
        jnText += aBuffer.toString() ;
        
        return jnText ;
    }
    
    private void outputProcessingResult() throws Exception {
        
        List<String> contents = new ArrayList<String>() ;
        contents.addAll( hdrLines ) ;
        contents.add( "" ) ;
        for( Question q : questions ) {
            contents.addAll( q.qLines ) ;
            contents.addAll( q.aLines ) ;
            contents.add( "" ) ;
        }
        print( "Contents", contents ) ;
    }
    
    private void print( String hdr, List<String> lines ) {
        logger.debug( hdr ) ;
        logger.debug( "---------------------------------------------------------------" ) ; 
        for( String line : lines ) {
            logger.debug( line ) ;
        }
    }
}
