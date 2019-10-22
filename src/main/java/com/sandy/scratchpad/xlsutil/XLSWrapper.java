package com.sandy.scratchpad.xlsutil;

import java.io.InputStream ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.poi.ss.usermodel.Sheet ;
import org.apache.poi.ss.usermodel.Workbook ;
import org.apache.poi.xssf.usermodel.XSSFWorkbook ;

public class XLSWrapper {
    
    private Workbook workbook = null ;
    
    public XLSWrapper( InputStream worksheetInputStream ) 
        throws Exception {
        this.workbook = new XSSFWorkbook( worksheetInputStream ) ;
    }
    
    public List<XLSRow> getRows() {
        return this.getRows( null, null ) ;
    }
    
    public List<XLSRow> getRows( String sheetName ) {
        return this.getRows( sheetName, null ) ;
    }
    
    public List<XLSRow> getRows( String sheetName, XLSRowFilter filter ) {
        
        List<XLSRow> rows = new ArrayList<>() ;
        
        Sheet sheet = getSheet( sheetName ) ;
        XLSSheetConfig sheetConfig = new XLSSheetConfig( sheet ) ;
        
        for( int i=1; i<=sheetConfig.getNumRows(); i++ ) {
            List<String> cellValues = XLSUtil.getCellValues( sheet.getRow( i ) ) ;
            XLSRow row = new XLSRow( cellValues, sheetConfig ) ;
            if( ( filter == null ) || 
                ( filter != null && filter.accept( row ) ) ) {
                sheetConfig.updateColSize( row ) ;
                rows.add( row ) ;
            }
        }
        return rows ;
    }
    
    public List<String> getColValues( String sheetName, 
                                      XLSRowFilter filter, 
                                      int colIndex ) {
        
        List<String> colValues = new ArrayList<>() ;
        List<XLSRow> rows = getRows( sheetName, filter ) ;
        if( rows.size() > 0 ) {
            for( XLSRow row : rows ) {
                colValues.add( row.getCellValue( colIndex ) ) ;
            }
        }
        return colValues ;
    }
    
    public List<String> getColValues( String sheetName, 
                                      XLSRowFilter filter, 
                                      String colName ) {
        
        List<String> colValues = new ArrayList<>() ;
        List<XLSRow> rows = getRows( sheetName, filter ) ;
        if( rows.size() > 0 ) {
            XLSSheetConfig config = rows.get( 0 ).getConfig() ;
            int colIndex = config.getColIndex( colName ) ;
            for( XLSRow row : rows ) {
                colValues.add( row.getCellValue( colIndex ) ) ;
            }
        }
        return colValues ;
    }
    
    private Sheet getSheet( String sheetName ) {
        
        Sheet sheet = null ;
        if( sheetName != null ) {
            sheet = workbook.getSheet( sheetName ) ;
        }
        else {
            sheet = workbook.getSheetAt( 0 ) ;
        }
        return sheet ;
    }
}
