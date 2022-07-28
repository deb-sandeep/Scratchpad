package com.sandy.scratchpad.jn.englang.preposition;

import java.util.Set ;
import java.util.TreeSet ;

public class PrepositionMaster {

    private static final String[] RAW_DATA = { 
        "about", "above", "across", "after", "ago", "at", "before", "behind", 
        "below", "beside", "besides", "by", "down", "during", "for", "from", 
        "in", "into", "near", "off", "on", "onto", "over", "past", "round", 
        "since", "through", "till", "to", "under", "until", "up", "with", 
        "within"
    } ;
    
    private static PrepositionMaster instance = new PrepositionMaster() ;
    
    private Set<String> prepositions = new TreeSet<>() ;
    
    private PrepositionMaster() {
        
        for( String data : RAW_DATA ) {
            prepositions.add( data ) ;
        }
    }
    
    public static PrepositionMaster instance() {
        return instance ;
    }
    
    public boolean isPresposition( String word ) {
        return prepositions.contains( word.toLowerCase() ) ;
    }
    
    public Set<String> getPrepositions() {
        return this.prepositions ;
    }
}
