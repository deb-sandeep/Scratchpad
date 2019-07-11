package com.sandy.scratchpad.jn.marathi;

import java.util.HashMap ;
import java.util.List ;

import org.apache.log4j.Logger ;

public class MarathiBasics {

    static final Logger log = Logger.getLogger( MarathiBasics.class ) ;
    
    private HashMap<String, List<MarathiFragment>> sections = new HashMap<>() ;
    
    ResourceLeecher leecher = new ResourceLeecher( sections ) ;
    ResourceDownloader downloader = new ResourceDownloader( sections ) ;
    ChapterCreator chapterCreator = new ChapterCreator( sections ) ;
    
    public void process() throws Exception {
        leecher.processUrls() ;
        downloader.downloadResources() ;
//        chapterCreator.createSectionChapters() ;
        
    }
    public static void main( String[] args ) throws Exception {
        MarathiBasics driver = new MarathiBasics() ;
        driver.process() ;
    }
}
