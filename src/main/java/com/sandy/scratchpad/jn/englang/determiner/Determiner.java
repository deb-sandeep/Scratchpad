package com.sandy.scratchpad.jn.englang.determiner;

import lombok.Data ;

@Data
public class Determiner {

    private String word = null ;
    private DeterminerType type = null ;
    
    public Determiner( String word, DeterminerType type ) {
        this.word = word ;
        this.type = type ;
    }
}
