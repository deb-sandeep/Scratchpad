package com.sandy.scratchpad.jee.qclassifier;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

public class FileHelper {
    
    public static File findDir( File dir, String name ) {
        if( dir == null || !dir.isDirectory() ) return null ;
        File[] children = dir.listFiles() ;
        if( children == null ) return null ;
        for( File child : children ) {
            if( child.isDirectory() ) {
                if( child.getName().equals( name ) ) return child ;
                File found = findDir( child, name ) ;
                if( found != null ) return found ;
            }
        }
        return null ;
    }
    
    public static File[] getQuestionImgFiles( File srcDir, String qImgHint ) {
        
        File qImgDir = new File( srcDir, "question-images" ) ;
        if( !qImgDir.isDirectory() ) return null ;
        
        File[] allFiles = qImgDir.listFiles( File::isFile ) ;
        if( allFiles == null ) return null ;
        
        return Arrays.stream( allFiles )
                .filter( f -> {
                    if( qImgHint == null || qImgHint.isBlank() )
                        return true ;
                    
                    String questionId = extractQuestionId( f ) ;
                    if( qImgHint.length() == 1 )
                        return questionId.startsWith( qImgHint ) ;
                    
                    return questionId.equals( qImgHint ) ;
                } )
                .sorted( Comparator.comparing( ( File f ) -> extractQuestionId( f ).split( "_" )[0] )
                        .thenComparing( f -> extractQuestionId( f ).split( "_" )[1] )
                        .thenComparingInt( f -> Integer.parseInt( extractQuestionId( f ).split( "_" )[2] ) ) )
                .toArray( File[]::new ) ;
    }
    
    public static String extractQuestionId( File f ) {
        String nameWithoutExt = f.getName().replaceAll( "\\.[^.]+$", "" ) ;
        String[] parts = nameWithoutExt.split( "\\." ) ;
        return parts[ parts.length - 1 ] ;
    }
}
