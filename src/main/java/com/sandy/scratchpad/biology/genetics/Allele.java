package com.sandy.scratchpad.biology.genetics;

public class Allele {

    public static enum TraitType { DOMINANT, RECESSIVE } ;
    
    private TraitType traitType = null ;
    
    public Allele( TraitType type ) {
        this.traitType = type ;
    }
    
    public boolean isDominant() {
        return this.traitType == TraitType.DOMINANT ;
    }
    
    public boolean isRecessive() {
        return !isDominant() ;
    }
    
    public static Allele generateRandom() {
        return Math.random() >= 0.5 ? new Allele( TraitType.DOMINANT ) :
                                      new Allele( TraitType.RECESSIVE ) ;
    }
}
