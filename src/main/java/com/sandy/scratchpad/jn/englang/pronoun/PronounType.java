package com.sandy.scratchpad.jn.englang.pronoun;

import java.util.ArrayList ;

import lombok.Data ;

@Data
public class PronounType {

    private String name = null ;
    private ArrayList<Pronoun> pronouns = new ArrayList<>() ;
    
    public PronounType( String name ) {
        this.name = name ;
    }

    public void addPronoun( Pronoun pronoun ) {
        pronouns.add( pronoun ) ;
    }
}
