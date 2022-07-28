package com.sandy.scratchpad.jn.englang.determiner;

import java.util.ArrayList ;

import lombok.Data ;

@Data
public class DeterminerType {

    private String name = null ;
    private ArrayList<Determiner> determiners = new ArrayList<>() ;
    
    public DeterminerType( String type ) {
        this.name = type ;
    }

    public void addDeterminer( Determiner d ) {
        determiners.add( d ) ;
    }
}
