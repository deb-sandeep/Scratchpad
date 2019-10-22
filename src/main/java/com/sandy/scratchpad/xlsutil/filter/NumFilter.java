package com.sandy.scratchpad.xlsutil.filter;

import com.sandy.scratchpad.xlsutil.XLSRow ;

public class NumFilter extends AbstractColFilter {
    
    public static final int GT = 1 ;
    public static final int LT = -1 ;
    public static final int EQ = 0 ;
    public static final int GTE = 2 ;
    public static final int LTE = -2 ;

    private int compareIndicator = 0 ;
    private double compareValue = 0 ;
    
    public NumFilter( int colIndex, 
                          int compareIndicator, 
                          double compareValue ) {
        super( colIndex ) ;
        this.compareIndicator = compareIndicator ;
        this.compareValue = compareValue ;
    }

    public NumFilter( String colName, 
                          int compareIndicator, 
                          double compareValue ) {
        super( colName ) ;
        this.compareIndicator = compareIndicator ;
        this.compareValue = compareValue ;
    }

    @Override
    public boolean accept( XLSRow row ) {
        
        Double cellValue = super.getNumericStringValue( row ) ;
        
        if( cellValue != null ) {
            switch( this.compareIndicator ) {
                case GT:
                    return cellValue > this.compareValue ;
                case LT:
                    return cellValue < this.compareValue ;
                case EQ:
                    return cellValue == this.compareValue ;
                case GTE:
                    return cellValue >= this.compareValue ;
                case LTE:
                    return cellValue <= this.compareValue ;
            }
        }
        return false ;
    }

}
