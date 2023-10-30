package com.sandy.scratchpad;

import java.io.File ;

class Boy {
    public static String getGender() {
        return "Male" ;
    }
}

class Girl {
    public String getGender() {
        return "Female" ;
    }
}

class Cat {
    public String getSpecies() {
        return "Felix" ;
    }
}

public class Scratch {

    public static void main( String[] args ) {
        System.out.println( Boy.getGender() ) ;
    }
}
