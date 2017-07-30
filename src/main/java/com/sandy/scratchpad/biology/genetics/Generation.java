package com.sandy.scratchpad.biology.genetics;

import java.util.ArrayList ;

import org.apache.log4j.Logger ;

import com.sandy.scratchpad.biology.genetics.Individual.Genotype ;

public class Generation {

    private static final Logger log = Logger.getLogger( Generation.class ) ;
    
    private ArrayList<Individual> individuals = new ArrayList<>() ;
    
    private int genNum = 0 ;
    
    private int numHomoRecessive = 0 ;
    private int numHomoDominant = 0 ;
    private int numHeterozygous = 0 ;
    
    public Generation( int num ) {
        this.genNum = num ;
    }
    
    public int getGenNum() {
        return this.genNum ;
    }
    
    public double getPercentHomoRecessive() {
        if( individuals.size() == 0 ) return 0 ;
        return ((double)((numHomoRecessive) * 100) )/individuals.size() ;
    }
    
    public double getPercentHomoDominant() {
        if( individuals.size() == 0 ) return 0 ;
        return ((double)((numHomoDominant) * 100) )/individuals.size() ;
    }
    
    public double getPercentHeterozygous() {
        if( individuals.size() == 0 ) return 0 ;
        return ((double)((numHeterozygous) * 100) )/individuals.size() ;
    }
    
    public double getPercentHomozygous() {
        if( individuals.size() == 0 ) return 0 ;
        return ((double)((numHomoRecessive+numHomoDominant) * 100) )/individuals.size() ;
    }
    
    public int getPopulation() {
        return individuals.size() ;
    }
    
    public int numUnmatedIndividuals() {
        int result = 0 ;
        for( int i=0; i<individuals.size(); i++ ) {
            if( !individuals.get( i ).hasMated() ) {
                result++ ;
            }
        }
        return result ;
    }
    
    public void addIndividual( Individual individual ) {
        
        this.individuals.add( individual ) ;
        if( individual.getGenotye() == Genotype.HOMO_DOMINANT ) {
            numHomoDominant++ ;
        }
        else if( individual.getGenotye() == Genotype.HOMO_RECESSIVE ) {
            numHomoRecessive++ ;
        }
        else {
            numHeterozygous++ ;
        }
    }

    public Generation generateNextGeneration() {
        
        Generation newGen = new Generation( this.genNum+1 ) ;
        
        log.debug( "Generating gen " + genNum + " progeny." ) ;
        
        for( int i=0; i<individuals.size(); i++ ) {
            i = getNextUnmatedIndividual( i ) ;
            log.debug( "\tMale found at index " + i ) ;
            if( i!=-1 ) {
                Individual i1 = individuals.get( i ) ;
                Individual i2 = null ;
                int j = getRandomUnmatedIndividual( i+1 ) ;
                log.debug( "\tFemale found at index " + j ) ;
                if( j!=-1 ) {
                    i2 = individuals.get( j ) ;
                }
                else {
                    break ;
                }
                
                mate( i1, i2, newGen ) ;
                log.debug( "\t\t2 offsprings created" );
            }
            else {
                break ;
            }
        }
        
        return newGen ;
    }
    
    private int getNextUnmatedIndividual( int i ) {
        while( i < individuals.size() ) {
            if( !individuals.get( i ).hasMated() ) {
                return i ;
            }
            i++ ;
        }
        return -1 ;
    }
    
    private int getRandomUnmatedIndividual( int i ) {
        
        int numLeft = individuals.size()-i ;
        if( numLeft <=0 ) return -1 ;
        
        ArrayList<Integer> unmatedList = new ArrayList<>() ;
        for( int j=0; j<numLeft; j++ ) {
            if( !individuals.get( i+j ).hasMated() ) {
                unmatedList.add( i+j ) ;
            }
        }
        if( unmatedList.isEmpty() ) return -1 ;
        return unmatedList.get( (int)(Math.random()*unmatedList.size()) ) ;
    }
    
    private void mate( Individual i1, Individual i2, Generation newGen ) {
        
        //int numOffsprings = (int)(Math.random()*ReproductionConfig.MAX_OFFSPRINGS) + 1 ;
        int numOffsprings = ReproductionConfig.MAX_OFFSPRINGS ;
        for( int i=0; i<numOffsprings; i++ ) {
            Individual child = new Individual( i1.getRandomAllele(), i2.getRandomAllele() ) ;
            newGen.addIndividual( child ) ;
        }
        i1.setMated();
        i2.setMated();
    }
}
