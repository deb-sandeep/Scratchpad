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
    private FileWriter fw = null ;
    
    private File jnFolder = new File( "/Users/sandeep/Documents/StudyNotes/JoveNotes-IT/Sandeep/Cloud/01 - Provider Products" ) ;
    
    public void execute() throws Exception {
        Map<String, List<AWSProductMeta>> metaMap = null ;
        
        metaMap = getProductMeta() ;
        //createJoveNotes( metaMap ) ;
        log.debug( "Generating TOC" ) ;
        printTOC( metaMap ) ;
    }
    
    public void createJoveNotes( Map<String, List<AWSProductMeta>> metaMap ) 
        throws Exception {
        
        int subChapterNumber = 1 ;
        
        for( String category : metaMap.keySet() ) {
            log.debug( "Category = " + category ) ;
            List<AWSProductMeta> metaList = metaMap.get( category ) ;
            if( metaList.size() > 3 ) {
                createJN( category, metaList, subChapterNumber ) ;
                subChapterNumber++ ;
            }
        }
    }
    
    private void createJN( String category, 
                           List<AWSProductMeta> metaList, 
                           int subChapterNumber ) 
        throws Exception {
        
        File jnFile = getJNFile( category, subChapterNumber ) ;
        fw = new FileWriter( jnFile ) ;
        
        fw.write( "subject \"Cloud\"\n" ) ;
        fw.write( "chapterNumber 1." + subChapterNumber + "\n" ) ;
        fw.write( "chapterName \"AWS - " + category + "\"\n" ) ;
        fw.write( "\n" ) ;
        
        createMatchingQuestion( metaList ) ;
        
        for( AWSProductMeta meta : metaList ) {
            writeQuestion( "Which product does this?\n\n**" + meta.getShortDescription() + "**" ) ;
            writeAnswer( meta.getProductName() ) ;
        }
        
        for( AWSProductMeta meta : metaList ) {
            writeQuestion( "What does this product do??\n\n**" + meta.getProductName() + "**" ) ;
            writeAnswer( meta.getShortDescription() ) ;
        }
        
        fw.flush() ;
        fw.close() ;
    }
    
    private File getJNFile( String category, int subChapNum ) {
        
        String fileName = "1." + subChapNum + " - " ;
        fileName += category.replace( "&", "and" ) ;
        fileName += ".jn" ;
        
        File jnFile = new File( jnFolder, fileName ) ;
        return jnFile ;
    }
    
    private void createMatchingQuestion( List<AWSProductMeta> metaList )
        throws Exception {
        
        fw.write( "\n@match \"Match the following\" {\n" ) ;
        
        for( AWSProductMeta meta : metaList ) {
            fw.write( "\t\"" + meta.getProductName() + "\" = \"" + meta.getShortDescription() + "\"\n" ) ;
        }
        
        fw.write( "\n" ) ;
        fw.write( "\t@mcq_config {\n" ) ;
        fw.write( "\t\t@forwardCaption \"What does this product do?\"\n" ) ;
        fw.write( "\t\t@reverseCaption \"Which product does this?\"\n" ) ;
        fw.write( "\t\t@numOptionsToShow 4\n" ) ;
        fw.write( "\t\t@numOptionsPerRow 1\n" ) ;
        fw.write( "\t}\n" ) ;
        fw.write( "}\n" ) ;
     }
    
    private void writeQuestion( String question ) throws Exception {
        fw.write( "\n" ) ;
        fw.write( "@qa \"" + question + "\"\n" ) ;
    }
    
    private void writeAnswer( String answer ) throws Exception {
        fw.write( "\"" + answer + "\"\n" ) ;
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
    
    public void printTOC(  Map<String, List<AWSProductMeta>> metaMap ) throws Exception {
        
        int totalProducts = 0 ;
        for( String category : metaMap.keySet() ) {
            List<AWSProductMeta> metaList = metaMap.get( category ) ;
            log.debug( category + " (" + metaList.size() + ")" ) ;
            totalProducts += metaList.size() ;
            for( AWSProductMeta meta : metaList ) {
                log.debug( "\t" + meta.getProductName() ) ;
                log.debug( "\t\t" + meta.getShortDescription() ) ;
                log.debug( "" ) ;
            }
        }
        
        log.debug( "Total categories = " + metaMap.size() ) ;
        log.debug( "Total products = " + totalProducts ) ;
    }
    
    public static void main( String[] args ) throws Exception {
        new AWSProductsSummarizer().execute() ;
    }
}
