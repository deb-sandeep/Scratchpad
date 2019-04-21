package com.sandy.scratchpad.maths;

public class TOE_LCT_46 {

    public void simulate() throws Exception {
        
        for( int a=1; a<8; a++ ) {
            for( int b=2*a+1; b<4*a; b++ ) {
                for( int c=a+1; c<4*a; c++ ) {
    
                    double D = Math.pow( b, 2 ) - 4*a*c ;
                    if( D > 0 && D < Math.pow( a, 2 ) ) {
                        double roots[] = getRoots( a, -b, c ) ;
                        if( roots != null ) {
                            checkResultValidity( a, b, c, roots ) ;
                        }
                    }
                }
            }
        }
    }
    
    private boolean checkResultValidity( int a, int b, int c, double[] roots ) {
        
        String valid = ( roots[0] > 1 && roots[1] < 2 ) ? "VALID" : "INVALID" ;
        boolean isValid = valid.equals( "VALID" ) ;
        
        if( isValid ) {
            String fmtOutput = String.format( "%-10s a=%1d, b=%2d, c=%2d, alpha=%1.2f, beta=%1.2f",
                    valid, a, b, c, roots[0], roots[1] ) ;
            System.out.println( fmtOutput ) ;
        }
        return isValid ;
    }
    
    private double[] getRoots( double a, double b, double c ) {
        double D = Math.pow( b, 2 ) - 4*a*c ;
        if( D < 0 ) {
            return null ;
        }
        double roots[] = new double[2] ;
        roots[0] = ( -b - Math.sqrt( D ) )/(2*a) ;
        roots[1] = ( -b + Math.sqrt( D ) )/(2*a) ;
        return roots ;
    }

    public static void main( String[] args ) throws Exception {
        new TOE_LCT_46().simulate() ;
    }
}
