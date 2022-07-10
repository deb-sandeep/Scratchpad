package com.sandy.scratchpad.jn.englang.pronoun;

import lombok.Data ;

@Data
public class Pronoun {

    private String word = null ;
    private PronounType type = null ;
    
    public Pronoun( String word, PronounType type ) {
        this.word = word ;
        this.type = type ;
    }
}
