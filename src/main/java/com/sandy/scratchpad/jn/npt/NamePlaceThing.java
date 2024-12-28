package com.sandy.scratchpad.jn.npt;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

public class NamePlaceThing {
    
    private static final Logger log = Logger.getLogger( NamePlaceThing.class ) ;
    
    private static String NPT_CHAPTER_PATH = "/Users/sandeep/Documents/StudyNotes/JoveNotes-Std-X/Class-X/History/16 - Picture Recognition" ;
    
    private static class ImgMeta {
        static enum ImgType { NAME, PLACE, EVENT, THING } ;
        
        ImgType imgType = ImgType.NAME ;
        String fileName ;
        String displayName ;
        
        ImgMeta( String fileName ) {
            this.fileName = fileName ;
            parseFileName() ;
        }
        
        private void parseFileName() {
            String[] fNameParts = fileName.substring( 0, fileName.length()-4 ).split( "_" ) ;
            parseImgType( fNameParts[0] ) ;
            prepareDisplayName( fNameParts ) ;
        }
        
        private void parseImgType( String tag ) {
            switch( tag ) {
                case "place":
                    imgType = ImgType.PLACE ;
                    break ;
                case "evt":
                    imgType = ImgType.EVENT ;
                    break ;
                case "thing":
                    imgType = ImgType.THING ;
                    break ;
            }
        }
        
        private void prepareDisplayName( String[] fNameParts ) {
            String str = "" ;
            for( int i=0; i<fNameParts.length; i++ ) {
                String displayStr = toDisplayString( fNameParts[i] ) ;
                if( i==0 ) {
                    if( imgType == ImgType.NAME ) {
                        str += displayStr ;
                    }
                }
                else {
                    str += displayStr ;
                }
            }
            displayName = str.trim() ;
        }
        
        private String toDisplayString( String str ) {
            if( str.length() == 1 ) {
                return str.toUpperCase() + "." ;
            }
            else if( str.length() == 2 ) {
                if( str.equalsIgnoreCase( "of" ) ||
                    str.equalsIgnoreCase( "in" ) ||
                    str.equalsIgnoreCase( "as" ) ) {
                    return " " + str ;
                }
                else if( str.equalsIgnoreCase( "dr" ) ) {
                    return " Dr." ;
                }
                return " " + str.toUpperCase() ;
            }
            return " " + StringUtils.capitalize( str ) ;
        }
        
        private String getImgType() {
            switch( imgType ) {
                case NAME: return "person" ;
                case PLACE: return "place" ;
                case EVENT: return "event" ;
                case THING: return "thing" ;
            }
            return "" ;
        }
    }
    
    public static void main( String[] args ) {
        NamePlaceThing util = new NamePlaceThing() ;
        util.makeQuestions() ;
    }
    
    private void makeQuestions() {
        
        List<ImgMeta> metaList = getImgFiles() ;
        for( ImgMeta meta : metaList ) {
            String question = "@qa \"Identify the " + meta.getImgType() + ":  \n" +
                              "{{@img " + meta.fileName + "}}\"" ;
            String answer = "\"" + meta.displayName + "\"\n" ;
            
            log.debug( question ) ;
            log.debug( answer ) ;
        }
    }

    private List<ImgMeta> getImgFiles() {
        File dir = new File( NPT_CHAPTER_PATH, "img" ) ;
        File[] files = dir.listFiles( ( dir1, name ) -> name.endsWith( ".png" ) ) ;
        ArrayList<ImgMeta> metaList = new ArrayList<>() ;
        for( File file : files ) {
            metaList.add( new ImgMeta( file.getName() ) ) ;
        }
        return metaList ;
    }
}
