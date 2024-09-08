package com.sandy.scratchpad.jn.progex;

import com.sandy.common.util.CommandLineExec;
import com.sandy.common.util.StringUtil;
import com.sandy.scratchpad.jn.exercise.ExerciseGen;
import com.sandy.scratchpad.jn.textextract.JNTextExtractor;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.WordUtils;
import org.apache.log4j.Logger;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ProgramExerciseGen {
    
    private static final Logger log = Logger.getLogger( JNTextExtractor.class ) ;
    
    public static File JN_DIR = new File( "/Users/sandeep/Documents/StudyNotes/JoveNotes-Std-X/Class-X/Computers" ) ;
    public static File GEN_SRC_DIR = new File( "/Users/sandeep/projects/learning/Std10_Programs/src/com/arunima/stdx" ) ;
    
    public static String[] ELIGIBLE_CHAPTERS = {
        "04 - User Defined Methods"
    } ;
    
    public static void main( String[] args ) throws Exception {
        
        ProgramExerciseGen gen = new ProgramExerciseGen() ;
        for( String chapterName : ELIGIBLE_CHAPTERS ) {
            gen.generateSkeletonPrograms( chapterName ) ;
        }
    }
    
    private void generateSkeletonPrograms( String chapterName ) throws IOException {
        int jnChapterNum = getBaseChapterNum( chapterName ) ;
        File[] exImgFiles = getExerciseImageFiles( chapterName ) ;
        for( File exImgFile : exImgFiles ) {
            generateSkeletonProgram( jnChapterNum, exImgFile ) ;
        }
    }
    
    private void generateSkeletonProgram( int jnChapterNum, File exImgFile ) throws IOException {
        
        log.debug( "------------ " + exImgFile.getName() + " -------------" ) ;
        String imgText = getImgText( exImgFile ) ;
        String className = getExerciseClassName( exImgFile ) ;
        
        File srcFile = new File( GEN_SRC_DIR, "ch" + jnChapterNum + "/todo/" + className + ".java" ) ;
        FileUtils.write( srcFile, getJavaSource( jnChapterNum, className, imgText ) ) ;
    }
    
    private String getJavaSource( int chapterNumber, String className, String exerciseStmt ) {
        
        StringBuilder sb = new StringBuilder() ;
        sb.append( "package com.arunima.stdx.ch" + chapterNumber + ".todo ;\n" )
                .append( "\n" )
                .append( "import java.util.* ;\n" )
                .append( "\n" ) ;
        
        sb.append( "/**\n * " )
                .append( exerciseStmt.replace( "\n", "\n * " ) )
                .append( "\n */\n" ) ;
        
        sb.append( "public class " + className + "\n" )
                .append( "{\n" )
                .append( "    public static void main( String[] args )\n" )
                .append( "    {\n" )
                .append( "        System.out.println( \"----------- Program Title --------------\" ) ;\n" )
                .append( "\n" )
                .append( "        Scanner sc = new Scanner( System.in ) ;\n" )
                .append( "        System.out.println( \"<Prompt> : \" ) ;\n" )
                .append( "\n" )
                .append( "        // Logic\n\n" )
                .append( "        sc.close() ;\n" )
                .append( "    }\n" )
                .append( "}\n" ) ;
        
        return sb.toString() ;
    }
    
    private String getExerciseClassName( File exImgFile ) {
        String baseName = exImgFile.getName().substring( 0, exImgFile.getName().length()-4 ) ;
        String className = "Program_" + baseName.substring( baseName.indexOf( "_" ) + 1 ) ;
        return className ;
    }
    
    private int getBaseChapterNum( String chapterName ) {
        int index = chapterName.indexOf( '-' ) ;
        if( index == -1 ) {
            throw new IllegalArgumentException( "Chapter name invalid. - not found" ) ;
        }
        String str = chapterName.substring( 0, index ).trim() ;
        return Integer.parseInt( str ) ;
    }
    
    private File[] getExerciseImageFiles( String chapterName ) {
        File exImgFilesDir = new File( JN_DIR, chapterName + "/img/exercise" ) ;
        return exImgFilesDir.listFiles( new FilenameFilter() {
            public boolean accept( File dir, String name ) {
                if( name.endsWith( ".png" ) ) {
                    String baseName = name.substring( 0, name.length()-4 ) ;
                    return !baseName.contains( "(" ) &&
                           !baseName.contains( "." ) &&
                           !baseName.contains( "Hdr" );
                }
                return false ;
            }
        } ) ;
    }
    
    private String getImgText( File imgFile ) {
        
        String[] cmdArgs = generateCmdArgs( imgFile, "eng" ) ;
        List<String> text = new ArrayList<>() ;
        StringBuilder sb = new StringBuilder() ;
        
        CommandLineExec.executeCommand( cmdArgs, text ) ;
        
        boolean paragraphAdded = false ;
        
        for( String line : text ) {
            if( !StringUtil.isEmptyOrNull( line ) ) {
                sb.append( line.trim() ).append( " " );
                paragraphAdded = false ;
            }
            else {
                if( !paragraphAdded ) {
                    sb.append( "\n\n" ) ;
                    paragraphAdded = true ;
                }
            }
        }
        
        return WordUtils.wrap( sb.toString(), 80, "\n", false ) ;
    }
    
    private String[] generateCmdArgs( File imgFile, String lang ) {
        List<String> args = new ArrayList<>() ;
        args.add( "/opt/homebrew/bin/tesseract" ) ;
        args.add( imgFile.getAbsolutePath() ) ;
        args.add( "stdout" ) ;
        args.add( "-l" ) ;
        args.add( lang ) ;
        return args.toArray( new String[0] ) ;
    }
}
