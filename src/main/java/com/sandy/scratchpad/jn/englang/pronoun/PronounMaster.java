package com.sandy.scratchpad.jn.englang.pronoun;

import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

public class PronounMaster {

    private static final String[][] RAW_DATA = {
            { "Personal",       "I", "me", "she", "her", "he", "him", "you", "we", "us", "it", "they", "them" },
            { "Possessive",     "my", "mine", "ours", "his", "hers", "its", "theirs", "yours" },
            { "Demonstrative",  "this", "these", "that", "those", "yonder" },
            { "Interrogative",  "who", "whom", "what", "which", "why", "where", "when", "whatever" },
            { "Reflexive",      "myself", "yourself", "himself", "herself", "itself", "oneself", "ourselves", "yourselves", "themselves" },
            { "Emphasising",    "myself", "herself" },
            { "Distributive",   "each", "either", "neither" },
            { "Indefinite",     "several", "all", "both", "anyone", "everyone", "someone", "everybody", "nobody", "one" },
            { "Reciprocal",     "one another", "each other" },
            { "Relative",       "that", "which", "who", "whose", "whom", "where", "when" }            
    } ;
    
    private static PronounMaster instance = new PronounMaster() ;
    
    private Map<String, PronounType> typesMap = new HashMap<>() ;
    private Map<String, Pronoun> pronounMap = new HashMap<>() ;
    
    private List<Pronoun> pronouns = new ArrayList<>() ;
    
    private PronounMaster() {
        
        for( String[] data : RAW_DATA ) {
            PronounType type = null ;
            for( int i=0; i<data.length; i++ ) {
                if( i==0 ) {
                    type = new PronounType( data[0] ) ;
                    typesMap.put( data[0], type ) ;
                }
                else {
                    Pronoun pronoun = new Pronoun( data[i], type ) ;
                    pronouns.add( pronoun ) ;
                    pronounMap.put( pronoun.getWord().toLowerCase(), pronoun ) ;
                    
                    type.addPronoun( pronoun ) ;
                }
            }
        }
    }
    
    public static PronounMaster instance() {
        return instance ;
    }
    
    public boolean isPronoun( String word ) {
        return pronounMap.containsKey( word.toLowerCase() ) ;
    }
    
    public Pronoun getPronoun( String word ) {
        return pronounMap.get( word.toLowerCase() ) ;
    }
}
