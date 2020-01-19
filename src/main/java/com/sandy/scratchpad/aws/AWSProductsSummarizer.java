package com.sandy.scratchpad.aws;

import java.io.FileWriter ;
import java.net.URI ;
import java.util.List ;
import java.util.Map ;

import org.apache.log4j.Logger ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.select.Elements ;

public class AWSProductsSummarizer {
    
    private static final Logger log = Logger.getLogger( AWSProductsSummarizer.class ) ;
    
    public void execute() throws Exception {
        Map<String, List<AWSProductMeta>> metaMap = null ;
        metaMap = new ProductMetaParser().parseMeta() ;
        
        FileWriter fw = new FileWriter( "/home/sandeep/temp/AWSProducts.txt" ) ;
        for( String category : metaMap.keySet() ) {
            log.debug( "Category = " + category ) ;
            fw.write( "Category : " + category + "\n" + 
                      "------------------------------------------\n\n" ) ; 
            
            List<AWSProductMeta> metaList = metaMap.get( category ) ;
            for( AWSProductMeta meta : metaList ) {
                captureLongDescription( meta ) ;
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
    
    private void captureLongDescription( AWSProductMeta meta ) 
        throws Exception {
        
        log.debug( "Fetching " + 
                   meta.getProductCategory() + 
                   ">" 
                   + meta.getProductName() ) ;
        
        Document doc = Jsoup.parse( new URI( meta.getUrl() ).toURL(), 5000 ) ;
        Elements descDivs = doc.select( "main>div" ) ;
        for( Element div : descDivs ) {
            StringBuilder builder = new StringBuilder() ;
            Elements descriptionParas = div.select( "p" ) ;
            if( descriptionParas.isEmpty() ) {
                continue ;
            }
            
            for( Element para : descriptionParas ) {
                builder.append( para.text() ).append( "\n\n" ) ;
            }
            meta.setLongDescription( builder.toString() ) ;
            break ;
        }
    }

    public static void main( String[] args ) throws Exception {
        new AWSProductsSummarizer().printTOC() ;
    }
}
