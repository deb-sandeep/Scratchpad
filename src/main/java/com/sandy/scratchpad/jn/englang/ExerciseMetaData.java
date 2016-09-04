package com.sandy.scratchpad.jn.englang;

import java.net.URL ;
import java.util.Date ;

public class ExerciseMetaData {

    private Date   publishDate = null ;
    private String description = null ;
    private URL    url         = null ;
    
    public Date getPublishDate() {
        return publishDate ;
    }
    
    public void setPublishDate( Date publishDate ) {
        this.publishDate = publishDate ;
    }
    
    public String getDescription() {
        return description ;
    }
    
    public void setDescription( String description ) {
        this.description = description ;
    }
    
    public URL getUrl() {
        return url ;
    }
    
    public void setUrl( URL url ) {
        this.url = url ;
    }
    
    public String toString() {
        StringBuilder buffer = new StringBuilder() ;
        buffer.append( GrammarExerciseTOC.SDF.format( publishDate ) )
              .append( " : " )
              .append( description )
              .append( " : " )
              .append( url.toString() ) ;
        return buffer.toString() ;
    }
}
