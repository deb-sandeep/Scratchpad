package com.sandy.scratchpad.xlsutil.filter;

import com.sandy.common.util.StringUtil ;
import com.sandy.scratchpad.xlsutil.XLSRow ;

public class StrFilter extends AbstractColFilter {

    private String regEx = null ;
    
    public StrFilter( int colIndex, String regEx ) {
        super( colIndex ) ;
        this.regEx = regEx ;
    }

    public StrFilter( String colName, String regEx ) {
        super( colName ) ;
        this.regEx = regEx ;
    }

    @Override
    public boolean accept( XLSRow row ) {
        String cellValue = super.getCellStringValue( row ) ;
        if( StringUtil.isNotEmptyOrNull( cellValue ) ) {
            return cellValue.matches( this.regEx ) ;
        }
        return false ;
    }
}
