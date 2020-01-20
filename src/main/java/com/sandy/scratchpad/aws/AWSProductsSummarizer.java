package com.sandy.scratchpad.aws;

import java.io.File ;
import java.io.FileInputStream ;
import java.io.FileOutputStream ;
import java.io.FileWriter ;
import java.io.ObjectInputStream ;
import java.io.ObjectOutputStream ;
import java.util.List ;
import java.util.Map ;

import org.apache.log4j.Logger ;

public class AWSProductsSummarizer {
    
    private static final Logger log = Logger.getLogger( AWSProductsSummarizer.class ) ;
    
    public void execute() throws Exception {
        Map<String, List<AWSProductMeta>> metaMap = null ;
        
        metaMap = getProductMeta() ;
        
        FileWriter fw = new FileWriter( "/home/sandeep/temp/AWSProducts.txt" ) ;
        for( String category : metaMap.keySet() ) {
            log.debug( "Category = " + category ) ;
            fw.write( "Category : " + category + "\n" + 
                      "------------------------------------------\n\n" ) ; 
            
            List<AWSProductMeta> metaList = metaMap.get( category ) ;
            for( AWSProductMeta meta : metaList ) {
                fw.write( "# " + meta.getProductName() + "\n\n" ) ;
                fw.write( "[" + meta.getProductCategory() + "]\n"  ) ;
                fw.write( "[" + meta.getUrl() + "]\n\n"  ) ;
                fw.write( meta.getShortDescription() + "\n\n" ) ;
                fw.write( meta.getLongDescription() + "\n" ) ;
                fw.flush() ;
            }
        }
        fw.flush() ;
        fw.close() ;
    }
    
    @SuppressWarnings( { "resource", "unchecked" } )
    private Map<String, List<AWSProductMeta>> getProductMeta() 
        throws Exception {
        
        Map<String, List<AWSProductMeta>> metaMap = null ;
        File persistedObjFile = new File( "/home/sandeep/temp/AWSProducts.obj" ) ;
        if( persistedObjFile.exists() ) {
            log.debug( "Loading persisted meta map." ) ;
            ObjectInputStream oIs = null ; 
            oIs = new ObjectInputStream( new FileInputStream( persistedObjFile ) ) ;
            metaMap = (Map<String, List<AWSProductMeta>>)oIs.readObject() ;
            oIs.close() ;
        }
        else {
            log.debug( "Loading live meta map." ) ;
            metaMap = new ProductMetaParser().parseMeta() ;
            ObjectOutputStream oOs = null ;
            oOs = new ObjectOutputStream( new FileOutputStream( persistedObjFile ) ) ;
            oOs.writeObject( metaMap ) ;
            oOs.flush() ;
            oOs.close() ;
        }
        return metaMap ;
    }
    
    public void printTOC() throws Exception {
        
        Map<String, List<AWSProductMeta>> metaMap = null ;
        metaMap = new ProductMetaParser().parseMeta() ;
        
        for( String category : metaMap.keySet() ) {
            log.debug( category ) ;
            List<AWSProductMeta> metaList = metaMap.get( category ) ;
            for( AWSProductMeta meta : metaList ) {
                log.debug( "\t" + meta.getProductName() ) ;
                log.debug( "\t\t" + meta.getShortDescription() ) ;
                log.debug( "" ) ;
            }
        }
    }
    
    public static void main( String[] args ) throws Exception {
        new AWSProductsSummarizer().execute() ;
    }
}
