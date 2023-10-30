package com.sandy.scratchpad.jee.blanknotes;

import org.apache.log4j.Logger;

import java.io.File;

public class BlankNotePDFGenerator {

    private static final Logger log = Logger.getLogger( BlankNotePDFGenerator.class ) ;

    private static String[][] SECTIONS = {
        { "a", "b", "c" },
    } ;

    public static void main(String[] args) throws Exception {

        Chapter chapter = new Chapter( "Mathematics", "Algebra", 2, "Theory of Equations" ) ;

        chapter.addSections( new String[]{
            "Definitions",
            "Remainder Theorem",
            "Factor Theorem",
            "Standard Identities",
            "Zeroes of Expression",
            "Roots of f(x) = g(x)",
            "Domain of Equation",
            "Extraneous Roots",
            "Loss of Root",
            "Graphs of polynomial fns",
            "Equations reducible to quadratic"
        } ) ;

        chapter.addSection( "Quadratic Equation", new String[]{
            "Quadratic with real coeffs",
            "Quadratic with non-real coeffs",
            "Range of quadratic",
            "Quadratic in two variables",
            "Relation between roots and coeffs of Quadratic"
        }) ;

        chapter.addSections( new String[]{
            "Symmetric Functions of Roots",
            "Common Roots",
            "Relation between root and coeffs of higher degree equations",
            "Quadratic function",
            "Rolle's Theorem",
            "Inequalities using location of roots"
        } ) ;

        log.debug( chapter ) ;

        File dir = new File( "/Users/sandeep/temp" ) ;
        chapter.generatePDF( dir ) ;
    }
}
