package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Re-syncs the processed-sources list with the sources' topic maps. Every
 * source in the source list - plus any extra name already in the processed
 * list - is checked: a listed source with an unassociated question is
 * removed, an unlisted source whose questions are all associated is added.
 * A source whose folder cannot be found is left as it is.
 */
@Slf4j
public final class ProcessedListRepair {

    /** Outcome of one repair run. */
    public static final class Report {
        int sourcesChecked ;
        int questionsChecked ;
        int imagesChecked ;
        int questionsUnassociated ;
        final List<String> added            = new ArrayList<>() ;
        final List<String> removed          = new ArrayList<>() ;
        final List<String> keptListed       = new ArrayList<>() ;   // complete, already listed
        final List<String> keptUnlisted     = new ArrayList<>() ;   // incomplete, not listed
        final List<String> notFound         = new ArrayList<>() ;   // folder missing - entry untouched
        final List<String> noImages         = new ArrayList<>() ;   // no question images (treated as incomplete)
        String error ;

        public List<String> getRemoved() { return removed ; }
        public boolean      hasChanges() { return !added.isEmpty() || !removed.isEmpty() ; }

        public String toText() {
            StringBuilder sb = new StringBuilder() ;
            if( error != null ) sb.append( "ERROR - processed list NOT updated: " ).append( error ).append( "\n\n" ) ;
            sb.append( String.format( "Sources checked          : %d%n", sourcesChecked ) ) ;
            sb.append( String.format( "Questions checked        : %d  (%d not associated)%n", questionsChecked, questionsUnassociated ) ) ;
            sb.append( String.format( "Images checked           : %d%n", imagesChecked ) ) ;
            sb.append( String.format( "Added to processed list  : %d%n", added.size() ) ) ;
            sb.append( String.format( "Removed from list        : %d%n", removed.size() ) ) ;
            sb.append( String.format( "Untouched                : %d  (%d complete & listed, %d incomplete & unlisted)%n",
                    keptListed.size() + keptUnlisted.size(), keptListed.size(), keptUnlisted.size() ) ) ;
            sb.append( String.format( "Folder not found         : %d%n", notFound.size() ) ) ;
            sb.append( String.format( "No question images       : %d%n", noImages.size() ) ) ;
            appendNames( sb, "Added", added ) ;
            appendNames( sb, "Removed", removed ) ;
            appendNames( sb, "Folder not found (entry left as is)", notFound ) ;
            appendNames( sb, "No question images", noImages ) ;
            return sb.toString() ;
        }

        private static void appendNames( StringBuilder sb, String title, List<String> names ) {
            if( names.isEmpty() ) return ;
            sb.append( '\n' ).append( title ).append( ":\n" ) ;
            names.forEach( n -> sb.append( "  " ).append( n ).append( '\n' ) ) ;
        }
    }

    private ProcessedListRepair() {}

    public static Report run( File qBankDir, List<String> sources, File processedListFile, String qIdHint ) {
        Report r = new Report() ;
        Set<String> processed = ProcessedList.load( processedListFile ) ;

        Set<String> toCheck = new LinkedHashSet<>( sources ) ;
        toCheck.addAll( processed ) ;

        Set<String> updated = new LinkedHashSet<>( processed ) ;
        for( String srcName : toCheck ) {
            boolean listed = processed.contains( srcName ) ;
            File srcDir = FileHelper.findDir( qBankDir, srcName ) ;
            if( srcDir == null ) {
                r.notFound.add( srcName ) ;
                continue ;
            }
            r.sourcesChecked++ ;

            Map<String,List<File>> groups = FileHelper.getQuestionImgGroups( srcDir, qIdHint ) ;
            boolean complete ;
            if( groups == null || groups.isEmpty() ) {
                r.noImages.add( srcName ) ;
                complete = false ;
            }
            else {
                Set<String> classified = WorkspaceTopicMap.classifiedQuestionIds( srcDir ) ;
                int unassociated = 0 ;
                for( Map.Entry<String,List<File>> e : groups.entrySet() ) {
                    r.questionsChecked++ ;
                    r.imagesChecked += e.getValue().size() ;
                    if( !classified.contains( e.getKey() ) ) unassociated++ ;
                }
                r.questionsUnassociated += unassociated ;
                complete = unassociated == 0 ;
            }

            if( complete && !listed )      { updated.add( srcName ) ;    r.added.add( srcName ) ; }
            else if( !complete && listed ) { updated.remove( srcName ) ; r.removed.add( srcName ) ; }
            else if( complete )            r.keptListed.add( srcName ) ;
            else                           r.keptUnlisted.add( srcName ) ;
        }

        if( r.hasChanges() ) {
            try {
                ProcessedList.rewrite( processedListFile, updated ) ;
            }
            catch( Exception e ) {
                log.error( "Error rewriting processed list file {}", processedListFile, e ) ;
                r.error = e.getMessage() ;
            }
        }
        log.info( "Processed list repair:\n{}", r.toText() ) ;
        return r ;
    }
}
