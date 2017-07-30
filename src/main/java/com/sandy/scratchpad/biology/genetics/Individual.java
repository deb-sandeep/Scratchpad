package com.sandy.scratchpad.biology.genetics;

public class Individual {

    public static enum Genotype { HOMO_DOMINANT, HETEROZYGOUS, HOMO_RECESSIVE } ;
    
    private Allele allele1 = null ;
    private Allele allele2 = null ;
    private boolean hasMated = false ;
    
    public Individual() {
        this.allele1 = Allele.generateRandom() ;
        this.allele2 = Allele.generateRandom() ;
    }
    
    public Individual( Allele a1, Allele a2 ) {
        this.allele1 = a1 ;
        this.allele2 = a2 ;
    }
    
    public Genotype getGenotye() {
        if( allele1.isDominant() && allele2.isDominant() ) {
            return Genotype.HOMO_DOMINANT ;
        }
        else if( allele1.isRecessive() && allele2.isRecessive() ) {
            return Genotype.HOMO_RECESSIVE ;
        }
        return Genotype.HETEROZYGOUS ;
    }
    
    public boolean hasMated() {
        return hasMated ;
    }
    
    public void setMated() {
        this.hasMated = true ;
    }
    
    public Allele getRandomAllele() {
        return Math.random() >= 0.5 ? allele1 : allele2 ;
    }
}
