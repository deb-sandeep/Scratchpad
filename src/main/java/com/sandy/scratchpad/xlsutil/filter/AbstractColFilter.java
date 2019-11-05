package com.sandy.scratchpad.xlsutil.filter;

import com.sandy.common.util.StringUtil ;
import com.sandy.scratchpad.xlsutil.XLSRow ;
import com.sandy.scratchpad.xlsutil.XLSRowFilter ;

public abstract class AbstractColFilter implements XLSRowFilter {
    
    protected String colName = null ;
    protected int colIndex = -1 ;
    
    public AbstractColFilter( String colName ) {
        this.colName = colName ;
    }
    
    public AbstractColFilter( int colIndex ) {
        this.colIndex = colIndex ;
    }
    
    protected String getCellStringValue( XLSRow row ) {
        if( colIndex != -1 ) {
            return row.getCellValue( colIndex ) ;
        }
        else if( colName != null ) {
            return row.getCellValue( colName ) ;
        }
        return "XXX" ;
    }

    protected Double getNumericStringValue( XLSRow row ) {
        String strCellVal = getCellStringValue( row ) ;
        if( StringUtil.isEmptyOrNull( strCellVal ) ) {
            return null ;
        }
        return Double.parseDouble( strCellVal ) ;
    }
}
