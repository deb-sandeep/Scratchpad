package com.sandy.scratchpad.jee.blanknotes;

import org.apache.log4j.Logger;

public class BlankNotePDFGenerator {

    private static final Logger log = Logger.getLogger( BlankNotePDFGenerator.class ) ;

    public static void main(String[] args) throws Exception {

        Topic topic = new Topic( "Maths", "Algebra", 13, "Vector Algebra" ) ;
        topic.loadConfig() ;
        //topic.generatePDF() ;
    }
}
