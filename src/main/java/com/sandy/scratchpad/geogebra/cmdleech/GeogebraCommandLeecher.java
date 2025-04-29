package com.sandy.scratchpad.geogebra.cmdleech;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.File;
import java.net.URL;

/**
 * Leeches the Geogebra help content and puts them in the BASE_DIR. Once
 * this program has run, the GeogebraMkSite app should be run to make the
 * navigation around the downloaded content.
 */
public class GeogebraCommandLeecher {
    
    private static final Logger log = Logger.getLogger( GeogebraCommandLeecher.class ) ;
    
    private static final String[] COMMAND_GROUPS = {
        "3D_Commands/:3D Commands",
        "Algebra_Commands/:Algebra Commands",
        "Chart_Commands/:Chart Commands",
        "Conic_Commands/:Conic Commands",
        "Discrete_Math_Commands/:Discrete Math Commands",
        "Functions_and_Calculus_Commands/:Function Commands",
        "Geometry_Commands/:Geometry Commands",
        "GeoGebra_Commands/:GeoGebra Commands",
        "List_Commands/:List Commands",
        "Logic_Commands/:Logic Commands",
        "Optimization_Commands/:Optimization Commands",
        "Probability_Commands/:Probability Commands",
        "Scripting_Commands/:Scripting Commands",
        "Spreadsheet_Commands/:Spreadsheet Commands",
        "Statistics_Commands/:Statistics Commands",
        "Financial_Commands/:Financial Commands",
        "Text_Commands/:Text Commands",
        "Transformation_Commands/:Transformation Commands",
        "Vector_and_Matrix_Commands/:Vector and Matrix Commands",
        "CAS_Specific_Commands/:CAS Specific Commands",
    } ;
    
    private static final String BASE_URL = "https://geogebra.github.io/docs/manual/en/commands/" ;
    
    public static final File BASE_DIR = new File( "/Users/sandeep/projects/workspace/geogebra/raw-content/" ) ;
    
    public static void main( String[] args ) throws Exception {
        
        GeogebraCommandLeecher app = new GeogebraCommandLeecher() ;
        
        for( String command : COMMAND_GROUPS ) {
            String[] cmdParts = command.split( ":" );
            app.leechCommandGroup( cmdParts[1], cmdParts[0] );
        }
    }
    
    private void leechCommandGroup( String groupName, String urlSuffix )
        throws Exception {
        
        log.debug( "Parsing command group: " + groupName ) ;
        
        URL url = new URL( BASE_URL + urlSuffix ) ;
        String content = IOUtils.toString( url.openStream() ) ;

        Document doc  = Jsoup.parse( content ) ;
        Element  body = doc.body() ;
        
        File dir = new File( BASE_DIR, groupName ) ;
        dir.mkdirs() ;
        
        Elements commands = body.select( "[class=\"ulist\"] li>p>a" ) ;
        for ( Element cmd : commands ) {
            String cmdUrlSuffix = cmd.attr( "href" ).substring( 3 ) ;
            String cmdName = cmd.text() ;
            leechCommand( cmdName, cmdUrlSuffix, dir ) ;
        }
    }
    
    private void leechCommand( String cmdName, String urlSuffix, File cmdGroupDir )
        throws Exception {
    
        log.debug( "    Parsing command: " + cmdName ) ;
        
        URL url = new URL( BASE_URL + urlSuffix ) ;
        String content = IOUtils.toString( url.openStream() ) ;
        
        Document doc  = Jsoup.parse( content ) ;
        Element  body = doc.body() ;
        
        Element bodyContent = body.select( "article[class=\"doc\"]" ).get( 0 ) ;
        File file = new File( cmdGroupDir, cmdName + ".html" ) ;
        FileUtils.writeStringToFile( file, bodyContent.toString() ) ;
    }
}
