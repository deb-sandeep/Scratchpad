package com.sandy.scratchpad.atmega;

import java.io.File ;
import java.net.URL ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.WordUtils ;
import org.apache.log4j.Logger ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.select.Elements ;

public class InstructionSet {
    
    static final Logger log = Logger.getLogger( InstructionSet.class ) ;
    
    private static final String LANDING_PG_URL = 
           "http://avr.8b.cz/asmhelp/Html/ATmega32_ATmega16_instructions.html" ;
    
    private OperLookup opLookup = new OperLookup() ;
    
    public InstructionSet() {
    }
    
    public void execute() throws Exception {
        processLandingPage() ;
    }
    
    private void processLandingPage() throws Exception {
        URL url = new URL( LANDING_PG_URL ) ;
        Document doc = Jsoup.parse( url, 5000 ) ;
        Element body = doc.body() ;
        
        Elements sections = body.select( "h3" ) ;
        for( Element section : sections ) {
            String sectionName = section.text() ;
            Element table = section.nextElementSibling() ;
            
            processSection( sectionName, table ) ;
        }
    }
    
    private void processSection( String sectionName, Element table ) 
        throws Exception {
        
        log.debug( "Processing section " + sectionName );
        
        List<InstructionMeta> instructions = new ArrayList<InstructionMeta>() ;
        
        Elements rows = table.select( "tr" ) ;
        for( int rowId=0; rowId<rows.size(); rowId++ ) {
            if( rowId == 0 )continue ;
            
            Element row = rows.get( rowId ) ;
            InstructionMeta meta = new InstructionMeta( row.select( "td" ) ) ;
            instructions.add( meta ) ;
        }
        
        makeNotes( sectionName, instructions ) ;
    }
    
    private void makeNotes( String sectionName, 
                            List<InstructionMeta> instructions )
        throws Exception {
        
        File file = new File( "/home/sandeep/temp/" + sectionName + ".txt" ) ;
        StringBuilder buffer = new StringBuilder() ;
        
        makeTeacherNotes( instructions, buffer ) ;
        makeQuestions( instructions, buffer ) ;
        
        log.debug( buffer.toString() );
        
        FileUtils.write( file, buffer.toString() ) ;
    }
    
    private void makeTeacherNotes( List<InstructionMeta> instructions,
                                   StringBuilder buffer ) {
        
        for( InstructionMeta instr : instructions ) {
            
            log.debug( "\t@tn " + instr.mnemonic ) ;
            buffer.append( "\n" )
                  .append( "@tn \"{{@blue " + instr.mnemonic + "}} - " + instr.description + "\"\n" )
                  .append( "\"### {{@blue " + instr.mnemonic + "}}&nbsp;&nbsp;&nbsp;" + String.join( ", ", instr.operands ) + "\n\n" )
                  .append( WordUtils.wrap( instr.longDescription, 80 ) )
                  .append( "\n\n" );
            
            if( instr.operands != null && instr.operands.length > 0 ) {
                buffer.append( "#### Operands\n\n" ) ;
                for( String oper : instr.operands ) {
                    buffer.append( "* " + oper + " - " + opLookup.get( oper ) + "\n" ) ;
                }
                buffer.append( "\n" ) ;
            }
            
            buffer.append( "#### {{@green " + instr.operation + "}}" ) ;
            
            if( instr.example.length() > 0 ) {
                buffer.append( "\n\n```asm\n" )
                      .append( instr.example )
                      .append( "\n```\n" ) ;
            }
            
            buffer.append( "\"\n" ) ;
        }
    }
    
    private void makeQuestions( List<InstructionMeta> instructions,
                                StringBuilder buffer ) {
        
        log.debug( "\tMaking questions" ) ;

        buffer.append( "\n\n" ) ;
        for( InstructionMeta instr : instructions ) {
            makeQ1( instr, buffer ) ;
        }
        
        buffer.append( "\n\n" ) ;
        for( InstructionMeta instr : instructions ) {
            makeQ2( instr, buffer ) ;
        }
        
        buffer.append( "\n\n" ) ;
        for( InstructionMeta instr : instructions ) {
            makeQ3( instr, buffer ) ;
        }
    }
    
    // @fib - <descr> is represnted by opcode ____
    private void makeQ1( InstructionMeta instr, StringBuilder buffer ) {
        
        buffer.append( "@fib \"" )
              .append( instr.description + " " )
              .append( "{{@blue " + instr.operation + "}} " )
              .append( "is represented by opcode {0}.\"\n" ) ;
        buffer.append( "\"" + instr.mnemonic + "\"" ) ;
        buffer.append( "\n\n" ) ;
    }

    // @fib - <instr> is used to ___
    private void makeQ2( InstructionMeta instr, StringBuilder buffer ) {
        
        buffer.append( "@qa \"" )
              .append( "{{@green " + instr.mnemonic + " _" + String.join( ",", instr.operands ) + "_ }}"  )
              .append( "is used for?\"\n" ) ;
        buffer.append( "\"" + instr.description + "\n\n" )
              .append( "#### {{@green " + instr.operation + "}}\"" ) ;
        buffer.append( "\n\n" ) ;
    }

    // @qa  - What are the operands for the instruction - opcode
    private void makeQ3( InstructionMeta instr, StringBuilder buffer ) {

        if( instr.operands.length == 1 && instr.operands[0].equalsIgnoreCase( "None" ) ) {
            return ;
        }
        
        buffer.append( "@qa \"List the operands for " )
              .append( "{{@green " + instr.mnemonic + " _" + String.join( ",", instr.operands ) + "_ }}\""  ) ;
        
        buffer.append( "\n\"\n" ) ;
        for( String oper : instr.operands ) {
            buffer.append( "* " + oper + " - " + opLookup.get( oper ) + "\n" ) ;
        }
        buffer.insert( buffer.length()-1, '"' ) ;
        
        buffer.append( "\n\n" ) ;
    }
    
    public static void main( String[] args ) throws Exception {
        InstructionSet engine = new InstructionSet() ;
        engine.execute() ;
    }
}
