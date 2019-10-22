package com.sandy.scratchpad.xlsutil;

import java.util.ArrayList ;
import java.util.HashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.commons.lang.StringUtils ;
import org.apache.poi.ss.usermodel.Sheet ;

public class XLSSheetConfig {
    
    private String sheetName = "" ;
    private int numRows = 0 ;
    private int numCols = 0 ;
    private List<String> colNames = null ;
    private List<Integer> colSizes = new ArrayList<>() ;
    private Map<String, Integer> colNameIndexMap = new HashMap<>() ;

    XLSSheetConfig( Sheet sheet ) {
        this.sheetName = sheet.getSheetName() ;
        populateColNames( sheet ) ;
        this.numRows = sheet.getLastRowNum() ;
    }
    
    private void populateColNames( Sheet sheet ) {
        this.colNames = XLSUtil.getCellValues( sheet.getRow( 0 ) ) ;
        this.numCols = this.colNames.size() ;
        
        int index = 0 ;
        for( String colName : colNames ) {
            colNameIndexMap.put( colName, index ) ;
            colSizes.add( colName.length() ) ;
            index++ ;
        }
    }
    
    void updateColSize( XLSRow row ) {
        for( int i=0; i<this.numCols; i++ ) {
            String val = row.getCellValue( i ) ;
            if( val.length() > this.colSizes.get( i ) ) {
                this.colSizes.set( i, val.length() ) ;
            }
        }
    }
    
    public String getSheetName() {
        return this.sheetName ;
    }
    
    public int getNumCols() {
        return this.numCols ;
    }
    
    public int getNumRows() {
        return this.numRows ;
    }
    
    public List<String> getColNames() {
        return this.colNames ;
    }
    
    public int getColIndex( String colName ) {
        return this.colNameIndexMap.get( colName ) ;
    }
    
    public int getColSize( int index ) {
        return this.colSizes.get( index ) ;
    }
    
    public int getColSize( String colName ) {
        return this.getColSize( this.colNameIndexMap.get( colName ) ) ;
    }

    public String getHeader() {
        StringBuilder buffer = new StringBuilder() ;
        for( int i=0; i<colNames.size()-1; i++ ) {
            int colSize = this.getColSize( i ) ;
            buffer.append( StringUtils.rightPad( colNames.get( i ), colSize ) ) 
                  .append( " | " ) ;
        }
        buffer.append( colNames.get( colNames.size()-1 ) ) ;
        int len = buffer.length() ;
        buffer.append( "\n" ) ;
        buffer.append( StringUtils.repeat( "-", len ) ) ;
        return buffer.toString() ;
    }
}
