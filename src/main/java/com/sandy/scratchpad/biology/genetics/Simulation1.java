package com.sandy.scratchpad.biology.genetics;

import java.text.DecimalFormat ;

import org.apache.commons.lang.StringUtils ;
import org.apache.log4j.Logger ;

import com.sandy.scratchpad.biology.genetics.Allele.TraitType ;

public class Simulation1 {
    
    private static final Logger log = Logger.getLogger( Simulation1.class ) ;
    
    private static final int ROOT_POPULATION = 100 ;
    private static final int NUM_GENERATIONS = 30 ;
    
    private static final double PCT_xx = 99 ;
    private static final double PCT_XX = 1 ;
    
    private Generation rootGen = new Generation(0) ;
    
    public Simulation1() {
        
        int numXX = (int)(( PCT_XX / 100 ) * ROOT_POPULATION ) ;
        int numxx = (int)(( PCT_xx / 100 ) * ROOT_POPULATION ) ;
        int numXx = ROOT_POPULATION - numXX - numxx ;
        
        for( int i=0; i<numxx; i++ ) {
            rootGen.addIndividual( new Individual( new Allele( TraitType.RECESSIVE ), 
                                                   new Allele( TraitType.RECESSIVE ) ) ) ;
        }
        for( int i=0; i<numXX; i++ ) {
            rootGen.addIndividual( new Individual( new Allele( TraitType.DOMINANT ), 
                                                   new Allele( TraitType.DOMINANT ) ) ) ;
        }
        for( int i=0; i<numXx; i++ ) {
            rootGen.addIndividual( new Individual( new Allele( TraitType.RECESSIVE ), 
                                                   new Allele( TraitType.DOMINANT ) ) ) ;
        }
    }
    
    public void simulate() {
        logHeader();
        logGeneration( rootGen ) ;
        
        Generation prevGen = rootGen ;
        Generation nextGen = null ;
        
        for( int i=1; i<=NUM_GENERATIONS; i++ ) {
            nextGen = prevGen.generateNextGeneration() ;
            logGeneration( nextGen ) ;
            prevGen = nextGen ;
        }
    }
    
    public void logHeader() {
        
        StringBuffer buffer = new StringBuffer() ;
        buffer.append( StringUtils.rightPad( "Gen", 4 ) )
              .append( StringUtils.rightPad( "Pop", 6 ) )
              .append( StringUtils.rightPad( "XX",  6 ) )
              .append( StringUtils.rightPad( "xx",  8 ) )
              .append( StringUtils.rightPad( "Xx",  6 ) );
        log.info( buffer ) ;
    }
    
    static DecimalFormat df = new DecimalFormat( "##.##" );
    
    private void logGeneration( Generation gen ) {
        
        
        StringBuffer buffer = new StringBuffer() ;
        buffer.append( StringUtils.rightPad( "" + gen.getGenNum(), 4 ) )
              .append( StringUtils.rightPad( "" + gen.getPopulation(), 6 ) )
              .append( StringUtils.rightPad( df.format( gen.getPercentHomoDominant() ), 6 ) )
              .append( StringUtils.rightPad( df.format( gen.getPercentHomoRecessive()), 8 ) )
              .append( StringUtils.rightPad( df.format( gen.getPercentHeterozygous() ), 6 ) );
        log.info( buffer ) ;
    }

    public static void main( String[] args ) {
        new Simulation1().simulate() ;
    }
}
