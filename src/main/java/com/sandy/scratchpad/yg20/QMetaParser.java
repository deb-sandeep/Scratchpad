package com.sandy.scratchpad.yg20;

import java.io.InputStream ;
import java.util.ArrayList ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;
import com.univocity.parsers.csv.CsvParser ;
import com.univocity.parsers.csv.CsvParserSettings ;

public class QMetaParser {

    static final Logger log = Logger.getLogger( QMetaParser.class ) ;
    
    public List<QMeta> parseQuestionMeta() {
        
        List<QMeta> metaList = new ArrayList<>() ;
        List<String[]> records = getRecords() ;
        
        String lastQType = null ;
        
        for( String[] record : records ) {
            String sub   = record[0] ;
            int    qNo   = Integer.parseInt( record[1] ) ;
            String qType = record[2] ;
            String ans   = record[3] ;
            
            if( StringUtil.isEmptyOrNull( qType ) ) {
                if( StringUtil.isEmptyOrNull( lastQType ) ) {
                    throw new IllegalArgumentException( "Unknown question type" ) ;
                }
                qType = lastQType ;
            }
            lastQType = qType ;
            
            if( StringUtil.isNotEmptyOrNull( ans ) ) {
                QMeta meta = new QMeta( sub, qNo, qType, ans ) ;
                metaList.add( meta ) ;
                //log.debug( meta ) ;
            }
        }
        return metaList ;
    }
    
    private List<String[]> getRecords() {
        
        CsvParserSettings settings = new CsvParserSettings() ;
        CsvParser parser = new CsvParser( settings ) ;
        InputStream is = YG20QProcessor.class.getResourceAsStream( "/YG201-answers.csv" ) ;
        return parser.parseAll( is ) ;
    }
}
