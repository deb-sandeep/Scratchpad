package com.sandy.scratchpad.jn.poem;

import java.io.File ;
import java.text.DecimalFormat ;
import java.util.ArrayList ;
import java.util.Iterator ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.commons.io.FileUtils ;
import org.apache.commons.lang.StringUtils ;

public class PoemFIB {

    private File file = null ;
    
    private Map<Integer, List<String>> paragraphs = new LinkedHashMap<>() ;
    private List<PoemParagraphFIB> paraFIBs = new ArrayList<>() ;

    public PoemFIB( File file ) {
        this.file = file ;
    }

    private void initialize() throws Exception {
        
        if( !file.exists() ) {
            throw new Exception( "File " + file.getAbsolutePath() + 
                                 "  does not exist." ) ;
        }
        
        int currentPara = 0 ;
        
        List<String> lines = FileUtils.readLines( file ) ;
        
        for( Iterator<String> iter = lines.iterator(); iter.hasNext(); ) {
            String line = iter.next() ;
            if( StringUtils.isEmpty( line.trim() ) ) {
                iter.remove() ;
            }
            else if( line.trim().equals( "@Para" ) ) {
                currentPara++ ;
            }
            else {
                List<String> paraLines = getParaLines( currentPara ) ;
                paraLines.add( line ) ;
            }
        }
        
        DecimalFormat df = new DecimalFormat( "00" ) ;
        
        for( Integer paraNum : paragraphs.keySet() ) {
            lines = paragraphs.get( paraNum ) ;
            paraFIBs.add( new PoemParagraphFIB( df.format( paraNum ) , lines ) ) ;
        }
        
    }
    
    private List<String> getParaLines( int paraNum ) {
        List<String> lines = paragraphs.get( paraNum ) ;
        if( lines == null ) {
            lines = new ArrayList<>() ;
            paragraphs.put( paraNum, lines ) ;
        }
        return lines ;
    }
    
    public void createFIBs() {
        for( PoemParagraphFIB fibs : paraFIBs ) {
            fibs.createFIBs();
        }
        for( PoemParagraphFIB fibs : paraFIBs ) {
            fibs.createBlankLineFIBs();
        }
    }
    
    public static void main( String[] args ) throws Exception {
        File poemFile = getPoemFile() ;
        PoemFIB driver = new PoemFIB( poemFile ) ;
        driver.initialize() ;
        driver.createFIBs() ;
    }
    
    public static File getPoemFile() {
        
        File userHomeDir = new File( System.getProperty("user.home") ) ;
        return new File( userHomeDir, "poem.txt" ) ;
    }
}
