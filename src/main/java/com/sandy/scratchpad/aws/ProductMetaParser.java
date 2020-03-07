package com.sandy.scratchpad.aws;

import java.io.InputStream ;
import java.net.URI ;
import java.util.ArrayList ;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;

import org.apache.commons.io.IOUtils ;
import org.apache.log4j.Logger ;
import org.jsoup.Jsoup ;
import org.jsoup.nodes.Document ;
import org.jsoup.nodes.Element ;
import org.jsoup.select.Elements ;

import com.sandy.common.util.StringUtil ;

public class ProductMetaParser {
    
    static final Logger log = Logger.getLogger( ProductMetaParser.class ) ;
    
    private List<String> lines = new ArrayList<>() ;
    private Map<String, List<AWSProductMeta>> map = new LinkedHashMap<>() ;

    public Map<String, List<AWSProductMeta>> parseMeta() throws Exception {
        
        InputStream is = getClass().getResourceAsStream( "/aws-products.txt" ) ;
        lines = IOUtils.readLines( is ) ;
        buildMeta() ;
        return map ;
    }
    
    private void buildMeta() throws Exception {
        
        String currentCategory = null ;
        AWSProductMeta currentMeta = null ;
        
        for( String line : lines ) {
            if( StringUtil.isEmptyOrNull( line ) ) continue ;
            
            line = line.trim() ;
            line = line.replace( "&amp;", "&" ) ;
            
            if( line.startsWith( "@" ) ) {
                currentCategory = line.substring( 1 ) ;
            }
            else if( line.startsWith( "#" ) ) {
                if( currentMeta != null ) {
                    storeMetaInMap( currentMeta ) ;
                }
                currentMeta = new AWSProductMeta() ;
                currentMeta.setProductCategory( currentCategory ) ;
                currentMeta.setProductName( line.substring( 1 ) ) ;
            }
            else if( line.startsWith( "$" ) ) {
                currentMeta.setShortDescription( line.substring( 1 ) ) ;
            }
            else if( line.startsWith( "/" ) ) {
                currentMeta.setUrl( "https://aws.amazon.com" + line ) ;
                captureLongDescription( currentMeta ) ;
            }
            else {
                throw new RuntimeException( "Unknown line - " + line ) ;
            }
        }
    }

    private void storeMetaInMap( AWSProductMeta meta ) {
        List<AWSProductMeta> metaList = map.get( meta.getProductCategory() ) ;
        if( metaList == null ) {
            metaList = new ArrayList<>() ;
            map.put( meta.getProductCategory(), metaList ) ;
        }
        metaList.add( meta ) ;
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
}
