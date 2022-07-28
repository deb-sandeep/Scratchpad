package com.sandy.scratchpad.jn.englang.determiner;

import java.util.ArrayList ;
import java.util.Collection ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

public class DeterminerMaster {

    private static final String[][] RAW_DATA = {
            { "Article",       "a", "an", "the" },
            { "Demonstrative", "this", "these", "that", "those" },
            { "Possessive",    "my", "our", "your", "his", "her", "its", "their" },
            { "Interrogative", "what", "which", "whose" },
            { "Quantifier",    "any", "much", "many", "several", "little", "some", "few" },
            { "Distributive",  "neither", "either", "every", "each" }
            
    } ;
    
    private static DeterminerMaster instance = new DeterminerMaster() ;
    
    private Map<String, DeterminerType> typesMap = new HashMap<>() ;
    private Map<String, Determiner> determinerMap = new HashMap<>() ;
    
    private List<Determiner> determiners = new ArrayList<>() ;
    
    private DeterminerMaster() {
        
        for( String[] data : RAW_DATA ) {
            DeterminerType type = null ;
            for( int i=0; i<data.length; i++ ) {
                if( i==0 ) {
                    type = new DeterminerType( data[0] ) ;
                    typesMap.put( data[0], type ) ;
                }
                else {
                    Determiner d = new Determiner( data[i], type ) ;
                    determiners.add( d ) ;
                    determinerMap.put( d.getWord().toLowerCase(), d ) ;
                    
                    type.addDeterminer( d ) ;
                }
            }
        }
    }
    
    public static DeterminerMaster instance() {
        return instance ;
    }
    
    public boolean isDeterminer( String word ) {
        return determinerMap.containsKey( word.toLowerCase() ) ;
    }
    
    public Determiner getDeterminer( String word ) {
        return determinerMap.get( word.toLowerCase() ) ;
    }
    
    public Collection<DeterminerType> getDeterminerTypes() {
        return typesMap.values() ;
    }
}
