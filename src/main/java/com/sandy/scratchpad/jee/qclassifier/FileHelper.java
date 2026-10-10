package com.sandy.scratchpad.jee.qclassifier;

import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class FileHelper {

    /** A part image's question id, e.g. {@code C_SCA_74(2)} -> base {@code C_SCA_74}, part 2. */
    private static final Pattern PART_ID = Pattern.compile( "^(.+)\\((\\d+)\\)$" ) ;

    /** Trailing question sequence number of a base id, e.g. {@code M_IVT_21} -> 21. */
    private static final Pattern SEQ_NO = Pattern.compile( "(\\d+)$" ) ;

    /**
     * Orders base question ids by their trailing sequence number, numerically
     * ({@code M_SCA_2 < M_IVT_21 < P_SCA_31 < C_SCA_74}). Ids without one sort
     * last; ties fall back to natural, then lexical, order.
     */
    public static final Comparator<String> QUESTION_ORDER =
            Comparator.comparingLong( FileHelper::sequenceNumber )
                      .thenComparing( FileHelper::naturalCompare )
                      .thenComparing( Comparator.naturalOrder() ) ;

    private static final Pattern IMG_EXT = Pattern.compile( "(?i).+\\.(png|jpe?g|gif|bmp)$" ) ;

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

    /**
     * Question images of a source, grouped by base question id and ordered by
     * {@link #QUESTION_ORDER}. A plain question maps to a single file; a question
     * split into part images ({@code X(1)}, {@code X(2)}, ...) maps to its parts
     * in ascending part order. Returns null if the source has no
     * {@code question-images} directory.
     */
    public static Map<String,List<File>> getQuestionImgGroups( File srcDir, String qImgHint ) {

        File qImgDir = new File( srcDir, "question-images" ) ;
        if( !qImgDir.isDirectory() ) return null ;

        File[] allFiles = qImgDir.listFiles( f -> f.isFile() && IMG_EXT.matcher( f.getName() ).matches() ) ;
        if( allFiles == null ) return null ;

        Map<String,List<File>> groups = new TreeMap<>( QUESTION_ORDER ) ;
        for( File f : allFiles ) {
            String questionId = extractQuestionId( f ) ;
            String baseId = baseQuestionId( questionId ) ;
            if( !matchesHint( qImgHint, questionId, baseId ) ) continue ;
            groups.computeIfAbsent( baseId, k -> new ArrayList<>() ).add( f ) ;
        }
        groups.values().forEach( parts ->
                parts.sort( Comparator.comparingInt( f -> partNumber( extractQuestionId( f ) ) ) ) ) ;
        return groups ;
    }

    private static boolean matchesHint( String qImgHint, String questionId, String baseId ) {
        if( qImgHint == null || qImgHint.isBlank() ) return true ;
        if( qImgHint.length() == 1 ) return baseId.startsWith( qImgHint ) ;
        return baseId.equals( qImgHint ) || questionId.equals( qImgHint ) ;
    }

    public static String extractQuestionId( File f ) {
        String nameWithoutExt = f.getName().replaceAll( "\\.[^.]+$", "" ) ;
        String[] parts = nameWithoutExt.split( "\\." ) ;
        return parts[ parts.length - 1 ] ;
    }

    /** {@code C_SCA_74(2)} -> {@code C_SCA_74}; ids without a part suffix are returned unchanged. */
    public static String baseQuestionId( String questionId ) {
        Matcher m = PART_ID.matcher( questionId ) ;
        return m.matches() ? m.group( 1 ) : questionId ;
    }

    /** {@code C_SCA_74(2)} -> 2; 0 for ids without a part suffix. */
    public static int partNumber( String questionId ) {
        Matcher m = PART_ID.matcher( questionId ) ;
        return m.matches() ? Integer.parseInt( m.group( 2 ) ) : 0 ;
    }

    /** Trailing sequence number of a base question id; {@code Long.MAX_VALUE} if it has none. */
    public static long sequenceNumber( String baseQuestionId ) {
        Matcher m = SEQ_NO.matcher( baseQuestionId ) ;
        if( !m.find() ) return Long.MAX_VALUE ;
        try { return Long.parseLong( m.group( 1 ) ) ; }
        catch( NumberFormatException e ) { return Long.MAX_VALUE ; }
    }

    /**
     * Natural-order comparison: runs of digits compare numerically, everything
     * else lexically. Tolerates ids like {@code C_SCA_43(1)} or {@code C_9} that
     * the old {@code split("_")[2]} / {@code parseInt} sort choked on.
     */
    static int naturalCompare( String a, String b ) {
        int i = 0, j = 0 ;
        while( i < a.length() && j < b.length() ) {
            char ca = a.charAt( i ), cb = b.charAt( j ) ;
            if( Character.isDigit( ca ) && Character.isDigit( cb ) ) {
                int si = i, sj = j ;
                while( i < a.length() && Character.isDigit( a.charAt( i ) ) ) i++ ;
                while( j < b.length() && Character.isDigit( b.charAt( j ) ) ) j++ ;
                String na = a.substring( si, i ).replaceFirst( "^0+(?=\\d)", "" ) ;
                String nb = b.substring( sj, j ).replaceFirst( "^0+(?=\\d)", "" ) ;
                if( na.length() != nb.length() ) return na.length() - nb.length() ;
                int cmp = na.compareTo( nb ) ;
                if( cmp != 0 ) return cmp ;
            }
            else {
                if( ca != cb ) return ca - cb ;
                i++ ; j++ ;
            }
        }
        return ( a.length() - i ) - ( b.length() - j ) ;
    }
}
