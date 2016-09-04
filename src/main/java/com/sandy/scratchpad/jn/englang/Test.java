package com.sandy.scratchpad.jn.englang;

import java.text.SimpleDateFormat ;

public class Test {

    private static SimpleDateFormat SDF = new SimpleDateFormat( "dd/MM/yy" ) ;

    public static void main( String[] args ) throws Exception {
        System.out.println( SDF.parseObject( "10/01/12" ) ) ;
    }
}
