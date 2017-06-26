package com.sandy.scratchpad.atmega;

import java.util.HashMap ;

@SuppressWarnings( "serial" )
public class OperLookup extends HashMap<String, String> {

    public OperLookup() {
        super.put( "Rd",  "Destination (and source) register in the register file" ) ;
        super.put( "Rr",  "Source register in the register file" ) ;
        super.put( "b",   "Constant (0-7), can be a constant expression" ) ;
        super.put( "s",   "Constant (0-7), can be a constant expression" ) ;
        super.put( "P",   "Constant (0-31/63), can be a constant expression" ) ;
        super.put( "K6",  "Constant (0-63), can be a constant expression" ) ;
        super.put( "K8",  "Constant (0-255), can be a constant expression" ) ;
        super.put( "k",   "Constant, value range depending on instruction. Can be a constant expression" ) ;
        super.put( "q",   "Constant (0-63), can be a constant expression" ) ;
        super.put( "Rdl", "R24, R26, R28, R30. For ADIW and SBIW instructions" ) ;
        super.put( "X",   "Indirect address registers (X=R27:R26)" ) ;
        super.put( "Y",   "Indirect address registers (Y=R29:R28)" ) ;
        super.put( "Z",   "Indirect address registers (Z=R31:R30)" ) ;        
    }
}
