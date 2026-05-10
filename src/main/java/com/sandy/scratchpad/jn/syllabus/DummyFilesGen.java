package com.sandy.scratchpad.jn.syllabus;

import com.sandy.common.util.StringUtil ;
import com.sandy.common.xlsutil.XLSRow ;
import com.sandy.common.xlsutil.XLSWrapper ;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger ;
import org.apache.xmlbeans.impl.common.IOUtil;

import java.io.File ;
import java.io.FileWriter;
import java.io.IOException;
import java.util.LinkedHashMap ;
import java.util.List ;
import java.util.Map ;

public class DummyFilesGen {
    
    private static final Logger log = Logger.getLogger( DummyFilesGen.class ) ;

    private File baseDir = new File( "/Users/sandeep/Documents/StudyNotes/JoveNotes-JEE/JEE" ) ;
    private File jnRoot = new File( baseDir, "JEE" ) ;

    private Map<String, Subject> subjectMap = new LinkedHashMap<>() ;
    
    private String currentSubjectName = null ;
    
    public void execute() throws Exception {
        File[] subjectFolders = baseDir.listFiles() ;
        assert subjectFolders != null;
        for( File subjectFolder : subjectFolders ) {
            if( subjectFolder.isDirectory() ) {
                log.debug( "Processing subject " + subjectFolder.getName() ) ;
                processSubject( subjectFolder ) ;
            }
        }
    }
    
    private void processSubject( File subjectFolder ) {
        String subjectName = subjectFolder.getName() ;
        File[] chapterFolders = subjectFolder.listFiles() ;
        for( File chapterFolder : chapterFolders ) {
            if( chapterFolder.isDirectory() ) {
                log.debug( "Processing chapter " + chapterFolder.getName() ) ;
                processChapter( subjectName, chapterFolder ) ;
            }
        }
    }
    
    private void processChapter( String subjectName, File chapterFolder ) {
        createDummyFile( new File( chapterFolder, "doc" ) ) ;
        createDummyFile( new File( chapterFolder, "img" ) ) ;
        createJNFile( subjectName, chapterFolder  );
    }
    
    private void createDummyFile( File dir ) {
        try {
            FileUtils.writeStringToFile( new File( dir, "dummy.txt" ), "Dummy file" ) ;
        }
        catch( IOException e ) {
            throw new RuntimeException( e ) ;
        }
    }
    
    private void createJNFile( String subjectName, File dir ) {
        String dirName = dir.getName() ;
        String[] parts = dirName.split( " - " ) ;
        int chapterNum = Integer.parseInt( parts[0] ) ;
        String chapterName = parts[1] ;
        
        try {
            File textChp = new File( dir, chapterNum + ".1 - " + chapterName + ".jn" ) ;
            
            if( textChp.exists() ) return ;
            
            FileWriter fw = new FileWriter( textChp ) ;
            
            fw.write( "@skip_generation_in_production\n" ) ;
            fw.write( "\n" ) ;
            fw.write( "subject \"" + subjectName + "\"\n" ) ;
            fw.write( "chapterNumber " + chapterNum + ".1\n" ) ;
            fw.write( "chapterName \"" + chapterName + "\"\n" ) ;
            fw.write( "\n" ) ;
            fw.write( "\n" ) ;
            fw.flush();
            fw.close();
        }
        catch( IOException ignored ) {}
    }
    
    public static void main( String[] args )
        throws Exception {
        DummyFilesGen gen = new DummyFilesGen() ;
        gen.execute() ;
    }
}
