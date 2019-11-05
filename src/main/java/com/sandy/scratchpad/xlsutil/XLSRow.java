package com.sandy.scratchpad.xlsutil;

import java.util.List ;

import org.apache.commons.lang.StringUtils ;

public class XLSRow {
    
    private XLSSheetConfig config = null ;
    private List<String> cellValues = null ;

    public XLSRow( List<String> cellValues, XLSSheetConfig sheetConfig ) {
        this.config = sheetConfig ;
        this.cellValues = cellValues ;
    }
    
    public XLSSheetConfig getConfig() {
        return this.config ;
    }
    
    public String getCellValue( int index ) {
        return cellValues.get( index ) ;
    }
    
    public String getCellValue( String colName ) {
        return getCellValue( config.getColIndex( colName ) ) ;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder() ;
        for( int i=0; i<cellValues.size()-1; i++ ) {
            int colSize = this.config.getColSize( i ) ;
            buffer.append( StringUtils.rightPad( cellValues.get( i ), colSize ) ) 
                  .append( " | " ) ;
        }
        buffer.append( cellValues.get( cellValues.size()-1 ) ) ;
        return buffer.toString() ;
    }
}
