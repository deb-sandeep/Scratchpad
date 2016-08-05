package com.sandy.scratchpad.jn.wm ;

import java.net.URLEncoder;
import java.util.ArrayList ;
import java.util.List ;

import com.wordnik.client.api.WordApi ;
import com.wordnik.client.model.Definition ;

public class WordnicAdapter {

    private String apiKey = "" ;
    
    public WordnicAdapter( String apiKey ) {
        this.apiKey = apiKey ;
    }
    
    public List<String> getDefinitions( String word ) 
            throws Exception {
        return getDefinitions( word, 1 ) ;
    }
    
    private List<String> getDefinitions( String word, int maxNumDefs ) 
            throws Exception {
        
        List<String> definitions = new ArrayList<>() ;
        
        WordApi api = new WordApi() ;
        api.getInvoker().addDefaultHeader( "api_key", this.apiKey ) ;
        
        List<Definition> defs = api.getDefinitions(
                URLEncoder.encode( word, "UTF-8" ),     
                null,     
                "all",    
                maxNumDefs,        
                "false",  
                "true",   
                "false"   
        ) ;
        
        for( Definition definition : defs ) {
            definitions.add( definition.getText() ) ;
        }
        
        return definitions ;
    }
}
