package com.sandy.scratchpad.physics;

import java.awt.Point ;
import java.util.ArrayList ;

public class Centrifugal {
    
    private static final double OMEGA    = 1 ;
    private static final double RADIUS   = 10 ;
    private static final double INTERVAL = 0.1 ;
    
    private static final int NUM_ITER = 100 ;
    
    private ArrayList<Double> refBodyDistance    = new ArrayList<Double>() ;
    private ArrayList<Double> bodyDistFromOrigin = new ArrayList<Double>() ;
    private ArrayList<Double> refPoints          = new ArrayList<Double>() ;
    
    private double velocityAtCircumference = OMEGA * RADIUS ;

    public void runSimulation() {

        computeRefBodyDistance() ;
        for( Double distance : refPoints ) {
            System.out.println( distance ) ;
        }
    }
    
    private void computeRefBodyDistance() {
        
        Point refPos  = null ;
        Point bodyPos = new Point() ;
        
        for( int i=0; i<NUM_ITER; i++ ) {
            double time = INTERVAL*i ;
            double refAngle = OMEGA * time ;
            refPos  = translatePolarToCoordinate( RADIUS, refAngle ) ;
            bodyPos.setLocation( RADIUS, velocityAtCircumference*time ) ;
            
            refBodyDistance.add( distance( refPos, bodyPos ) ) ;
            bodyDistFromOrigin.add( distance( new Point( 0, 0 ), bodyPos ) ) ;
            refPoints.add( 0.5*time*time*velocityAtCircumference*velocityAtCircumference/RADIUS ) ;
        }
    }
    
    private Point translatePolarToCoordinate( double r, double theta ) {
        Point point = new Point() ;
        point.setLocation( r * Math.cos( theta ),
                           r * Math.sin( theta ) ) ;
        return point ;
    }
    
    private double distance( Point a, Point b ) {
        return a.distance( b ) ;
    }
    
    public static void main( String[] args ) {
        new Centrifugal().runSimulation() ;
    }
}
