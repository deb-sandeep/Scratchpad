package com.sandy.scratchpad.jn.mathmock;

import java.io.File ;
import java.io.FileFilter ;
import java.io.FileWriter ;
import java.util.Arrays ;
import java.util.Comparator ;

public class XMathMockCreator {

    private static final String JN_SRC_DIR   = "/home/sandeep/Documents/StudyNotes/JoveNotes-X/Class-10/" ;
    private static final String JN_CHP_DIR   = JN_SRC_DIR + "Mathematics/201 - Mock Papers/" ;
    private static final String IMG_DIR_PATH = JN_CHP_DIR + "img" ;
    
    private XMathMockCreator() {}
    
    public void createMockTestPapers() throws Exception {
        for( int i=1; i<=20; i++ ) {
            createMockTestPaper( i ) ;
        }
    }
    
    private void createMockTestPaper( int paperNum ) throws Exception {
        
        String imgFolderName = "test-" + paperNum ;
        File imgFolder = new File( IMG_DIR_PATH + "/" + imgFolderName ) ;
        
        File[] pages = imgFolder.listFiles( new FileFilter() {
            @Override
            public boolean accept( File pathname ) {
                return pathname.getName().endsWith( ".jpg" ) ;
            }
        } ) ;
        
        Arrays.sort( pages, new Comparator<File>() {
            @Override
            public int compare( File o1, File o2 ) {
                return o1.getName().compareTo( o2.getName() ) ;
            }
        } );
        
        createJNFile( paperNum, pages ) ;
    }
    
    private void createJNFile( int paperNum, File[] pageImages ) 
            throws Exception {
            
        String fileName = "201." + paperNum + " - Mock Test " + paperNum + ".jn" ;
        File jnFile = new File( JN_CHP_DIR, fileName ) ;
        
        System.out.println( "Creating file " + jnFile.getAbsolutePath() ) ;

        FileWriter fw = new FileWriter( jnFile ) ;

        fw.write( "subject \"Mathematics\"\n" ) ;
        fw.write( "chapterNumber 201." + paperNum + "\n" ) ;
        fw.write( "chapterName \"Mock Test - " + paperNum + "\"\n" ) ;
        fw.write( "\n" ) ;
        fw.write( "@tn \"Mock Test - " + paperNum + "\"\n" ) ;
        fw.write( "\"{{@carousel\n" ) ;
        for( File imgFile : pageImages ) {
            fw.write( "    @slide test-" + paperNum + "/" + imgFile.getName() + "\n" ) ;
        }
        fw.write( "}}\"\n" ) ;
        fw.flush(); 
        fw.close();
    }
    
    public static void main( String[] args ) throws Exception {
        new XMathMockCreator().createMockTestPapers() ;
    }
}
