package com.sandy.scratchpad.jn.marathi;

import java.io.File ;
import java.io.FileWriter ;
import java.util.HashMap ;
import java.util.List ;

import org.apache.log4j.Logger ;

import com.sandy.common.util.StringUtil ;

// Image - Marathi (audio)
// Audio - English meaning
// English - Marathi (audio)
// Marathi - English meaning, audio, pronunciation
public class ChapterCreator {

    private static final Logger log = Logger.getLogger( ChapterCreator.class ) ;

    private static final File CHP_FOLDER = 
            new File( "/Users/sandeep/Documents/StudyNotes/JoveNotes-V/Class-5/Marathi/99 - Basics/" ) ;
    
    private static enum QType {
            IMG_AUDIO, 
            AUDIO_TRANSLATE,
            MARATHI_READ,
            SENTENCE_TRANSLATE
    } ;
    
    private HashMap<String, List<MarathiFragment>> sections = null ;
    
    public ChapterCreator( HashMap<String, List<MarathiFragment>> sections ) {
        this.sections = sections ;
    }
    
    public void createSectionChapters() 
        throws Exception {
        
        log.debug( "Creating section chapters ..." );
        int chapterNum = 1 ;
        for( String section : sections.keySet() ) {
            List<MarathiFragment> fragments = sections.get( section ) ;
            createSectionChapter( section, chapterNum, fragments ) ;
            chapterNum++ ;
        }
    }
    
    private void createSectionChapter( String sectionName, 
                                       int chapterNum,
                                       List<MarathiFragment> fragments ) 
        throws Exception {
        
        log.debug( "Creating chapter for = " + sectionName ) ;
        StringBuffer contentBuffer = new StringBuffer() ;
        
        for( QType qType : QType.values() ) {
            for( MarathiFragment fragment : fragments ) {
                String noteStr = createNotes( fragment, qType ) ;
                if( StringUtil.isNotEmptyOrNull( noteStr ) ) {
                    contentBuffer.append( noteStr ) ;
                    contentBuffer.append( "\n\n" ) ;
                }
            }
        }
        writePageChapter( sectionName, chapterNum, contentBuffer.toString() ) ;
    }
    
    private String createNotes( MarathiFragment fragment, QType qType ) {

        StringBuffer buffer = new StringBuffer() ;
        switch( qType ) {
            case IMG_AUDIO:
                createImage_MarathiNote( buffer, fragment ) ;
                break ;
            case AUDIO_TRANSLATE:
                createAudioTranslateNote( buffer, fragment ) ;
                break ;
            case MARATHI_READ:
                createMarathiReadingNote( buffer, fragment ) ;
                break ;
            case SENTENCE_TRANSLATE:
                createSentenceTranslationNote( buffer, fragment ) ;
                break ;
        }
        return buffer.toString() ;
    }
    
    private void createImage_MarathiNote( StringBuffer buffer, 
                                          MarathiFragment fragment ) {
        
        if( fragment.getImgResourcePath() == null ) return ;
        if( fragment.getAudioResourcePath() == null ) return ;
        
        String audioClip = getResourceFileName( fragment.getAudioResourcePath() ) ;
        String imgName = getResourceFileName( fragment.getImgResourcePath() ) ;
        
        buffer.append( "@qa \"Say in marathi\n" )
              .append( "\n" )
              .append( "{{@img " + imgName + "}}  \n" )
              .append( fragment.getEnglishText() + "\"" )
              .append( "\n" ) ;
        
        buffer.append( "\"{{@audio " +  audioClip + "}}\"" ) ;
    }
    
    private void createAudioTranslateNote( StringBuffer buffer,
                                           MarathiFragment fragment ) {
        
        if( fragment.getImgResourcePath() == null ) return ;
        if( fragment.getAudioResourcePath() == null ) return ;
        
        String audioClip = getResourceFileName( fragment.getAudioResourcePath() ) ;
        String imgName = getResourceFileName( fragment.getImgResourcePath() ) ;
        
        audioClip = audioClip.substring( 0, audioClip.lastIndexOf( '.' ) ) ;
        
        buffer.append( "@voice2text \"" + audioClip + "\"\n" ) ;
        
        buffer.append( "\"{{@img " + imgName + "}}  \n" )
              .append( fragment.getEnglishText() + "\"" ) ;
    }
    
    private void createMarathiReadingNote( StringBuffer buffer,
                                           MarathiFragment fragment ) {
        
        if( fragment.getAudioResourcePath() == null ) return ;
        
        String audioClip = getResourceFileName( fragment.getAudioResourcePath() ) ;
        
        buffer.append( "@qa \"Read in marathi and translate to english\n" )
              .append( "\n" )
              .append( "## " + fragment.getMarathiText() + "\"\n" ) ;
        buffer.append( "\"{{@audio " +  audioClip + "}}  \n" ) ;
        
        if( fragment.getPronunciation() != null ) {
            buffer.append( "_" + fragment.getPronunciation() + "_  \n" ) ;
        }
        
        buffer.append( fragment.getEnglishText() )
              .append( "\"" ) ;
    }
    
    private void createSentenceTranslationNote( StringBuffer buffer,
                                                MarathiFragment fragment ) {
        
        if( fragment.getAudioResourcePath() == null ) return ;
        
        String audioClip = getResourceFileName( fragment.getAudioResourcePath() ) ;
        
        buffer.append( "@qa \"Translate to marathi\n" )
              .append( "\n" )
              .append( "## " + fragment.getEnglishText() + "\"\n" ) ;
        buffer.append( "\"{{@audio " +  audioClip + "}}  \n" ) ;
        
        if( fragment.getPronunciation() != null ) {
            buffer.append( "_" + fragment.getPronunciation() + "_  \n" ) ;
        }
        
        buffer.append( "### " + fragment.getMarathiText() )
              .append( "\"" ) ;
    }

    private String getResourceFileName( String resPath ) {
        return resPath.substring( resPath.lastIndexOf( '/' ) + 1 ) ;
    }

    private void writePageChapter( String chapterName, int chapterNum,
                                   String content ) 
            throws Exception {
            
        File textChp = new File( CHP_FOLDER, "99." + chapterNum + " - Basics - " + chapterName + ".jn" ) ;

        FileWriter fw = new FileWriter( textChp ) ;

        fw.write( "@skip_generation_in_production\n" ) ;
        fw.write( "\n" ) ;
        fw.write( "subject \"Marathi\"\n" ) ;
        fw.write( "chapterNumber 99." + chapterNum + "\n" ) ;
        fw.write( "chapterName \"Basics - " + chapterName + "\"\n" ) ;
        fw.write( "\n" ) ;
        fw.write( content ) ;
        fw.flush(); 
        fw.close();
        
        log.debug( "File " + textChp.getAbsolutePath() + " created." ) ;
    }
}
