package com.sandy.scratchpad.jn.marathi;

import java.io.File ;
import java.util.HashMap ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.net.NetworkResourceDownloader ;

public class ResourceDownloader {

    private static final Logger log = Logger.getLogger( ResourceDownloader.class ) ;
    
    private static final File IMG_DEST_FOLDER = 
            new File( "/Users/sandeep/Documents/StudyNotes/JoveNotes-V/Class-5/Marathi/99 - Basics/img" ) ;

    private static final File AUDIO_DEST_FOLDER = 
            new File( "/Users/sandeep/Documents/StudyNotes/JoveNotes-V/Class-5/Marathi/99 - Basics/audio" ) ;
    
    private HashMap<String, List<MarathiFragment>> sections = null ;
    
    public ResourceDownloader( HashMap<String, List<MarathiFragment>> sections ) {
        this.sections = sections ;
    }
    
    public void downloadResources() {
        
        log.debug( "Downloading resouces..." );
        for( String section : sections.keySet() ) {
            log.debug( "Downloading resources for section = " + section ) ;
            List<MarathiFragment> fragments = sections.get( section ) ;
            for( MarathiFragment fragment : fragments ) {
                if( fragment.getAudioResourcePath() != null ) {
                    downloadClip( fragment.getAudioResourcePath() ) ;
                }
                if( fragment.getImgResourcePath() != null ) {
                    downloadImage( fragment.getImgResourcePath() ) ;
                }
            }
        }
    }
    
    private void downloadImage( String imgRes ) {
        File destFile = new File( IMG_DEST_FOLDER, getFileName( imgRes ) ) ;
        downloadResource( imgRes, destFile ) ;
    }
    
    private void downloadClip( String audioRes ) {
        File destFile = new File( AUDIO_DEST_FOLDER, getFileName( audioRes ) ) ;
        downloadResource( audioRes, destFile ) ;
    }
    
    private void downloadResource( String resPath, File destFile ) {
        
        if( destFile.exists() && destFile.length() != 0 ) {
            return ;
        }
        
        try {
            NetworkResourceDownloader downloader = null ;
            
            String url = "http://learn101.org/" + resPath ;
            downloader = new NetworkResourceDownloader( url ) ;
            if( downloader.execute() == 200 ) {
                downloader.saveResponseToFile( destFile ) ;
            }
            else {
                String msg = "Could not download sound clip from Google. msg=" + 
                             downloader.getStatusCode() + downloader.getReasonPhrase() ;
                log.info( "\t\t\t" + msg ) ;
            }
        }
        catch( Exception e ) {
            log.error( "Could not download " + resPath, e ) ;
        }
    }
    
    private String getFileName( String resPath ) {
        return resPath.substring( resPath.lastIndexOf( '/' ) + 1 ) ;
    }
}
