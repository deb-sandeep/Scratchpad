package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;

import static com.sandy.scratchpad.jee.qclassifier.FileHelper.*;

@Slf4j
public class QSourceClassifier {

    private static final Object PROCESSED_LIST_LOCK = new Object() ;

    private final File srcDir ;
    private final String qIdHint ;
    private final GemmaInvoker gemmaInvoker ;
    private final ExecutorService executor ;
    private final File processedListFile ;
    private final ClassificationListener listener ;
    private final BooleanSupplier keepGoing ;
    private final int maxInFlight ;

    public QSourceClassifier( File srcDir, String qIdHint, GemmaInvoker gemmaInvoker,
                              ExecutorService executor, File processedListFile,
                              ClassificationListener listener, BooleanSupplier keepGoing,
                              int maxInFlight ) {
        this.srcDir = srcDir ;
        this.qIdHint = qIdHint ;
        this.gemmaInvoker = gemmaInvoker ;
        this.executor = executor ;
        this.processedListFile = processedListFile ;
        this.listener = listener != null ? listener : ClassificationListener.NOOP ;
        this.keepGoing = keepGoing != null ? keepGoing : () -> true ;
        this.maxInFlight = Math.max( 1, maxInFlight ) ;
    }

    public String getSourceName() { return srcDir.getName() ; }

    /**
     * Fire {@code taskDiscovered} for every question image of this source and
     * return the tasks that still need classification.
     */
    public List<QTask> discover() {
        File[] qImgFiles = getQuestionImgFiles( srcDir, qIdHint ) ;
        if( qImgFiles == null ) {
            log.error( "No question images found for source {} and qIdHint {}", srcDir.getName(), qIdHint ) ;
            return List.of() ;
        }

        Set<String> classified = WorkspaceTopicMap.classifiedQuestionIds( srcDir ) ;
        List<QTask> pending = new ArrayList<>() ;
        for( File f : qImgFiles ) {
            String questionId = extractQuestionId( f ) ;
            QTask task = newTask( f, questionId ) ;
            boolean done = classified.contains( questionId ) ;
            listener.taskDiscovered( task, done ) ;
            if( !done ) pending.add( task ) ;
        }
        log.debug( "Source {} - {} of {} images pending", srcDir.getName(), pending.size(), qImgFiles.length ) ;
        return pending ;
    }

    /**
     * Classify the still-unclassified tasks, keeping at most {@code maxInFlight}
     * in the executor at any time. A new task is only submitted once an earlier
     * one completes AND {@code keepGoing} is still true - so "Stop" takes effect
     * after the currently-running tasks finish rather than after the whole
     * source's backlog drains.
     */
    public void process( List<QTask> pending ) {
        Set<String> classified = WorkspaceTopicMap.classifiedQuestionIds( srcDir ) ;
        Deque<QTask> queue = new ArrayDeque<>() ;
        for( QTask t : pending ) {
            if( !classified.contains( t.getQuestionId() ) ) queue.add( t ) ;
        }

        ExecutorCompletionService<Boolean> completion =
                new ExecutorCompletionService<>( executor ) ;
        int inFlight = 0 ;

        while( inFlight < maxInFlight && !queue.isEmpty() && keepGoing.getAsBoolean() ) {
            completion.submit( classifyCallable( queue.poll(), gemmaInvoker, listener, keepGoing ) ) ;
            inFlight++ ;
        }

        while( inFlight > 0 ) {
            try {
                completion.take().get() ;
            }
            catch( Exception e ) {
                log.error( "Error awaiting image result for source {}", srcDir.getName(), e ) ;
                if( e instanceof InterruptedException ) {
                    Thread.currentThread().interrupt() ;
                    break ;
                }
            }
            inFlight-- ;
            if( keepGoing.getAsBoolean() && !queue.isEmpty() ) {
                completion.submit( classifyCallable( queue.poll(), gemmaInvoker, listener, keepGoing ) ) ;
                inFlight++ ;
            }
        }

        maybeMarkProcessed() ;
    }

    /** Record the source as done only once every one of its images is classified. */
    private void maybeMarkProcessed() {
        File[] qImgFiles = getQuestionImgFiles( srcDir, qIdHint ) ;
        if( qImgFiles == null || qImgFiles.length == 0 ) return ;
        Set<String> classified = WorkspaceTopicMap.classifiedQuestionIds( srcDir ) ;
        for( File f : qImgFiles ) {
            if( !classified.contains( extractQuestionId( f ) ) ) return ;
        }
        markProcessed( srcDir.getName() ) ;
    }

    /**
     * Callable that classifies one image, persists the result into the task's
     * source workspace map, and fires listener events. Used by both the batch
     * pass and the UI re-queue action.
     */
    static Callable<Boolean> classifyCallable( QTask task, GemmaInvoker gemmaInvoker,
                                               ClassificationListener listener, BooleanSupplier keepGoing ) {
        return () -> {
            listener.taskStarted( task ) ;
            try {
                GemmaInvoker.InvocationResult r = gemmaInvoker.invoke( task.getImgFile(), keepGoing ) ;
                if( r.topics != null ) {
                    WorkspaceTopicMap.write( task.getSrcDir(), task.getQuestionId(), r.topics ) ;
                    listener.taskFinished( task, TaskStatus.DONE, r.attempts, summarise( r.topics ) ) ;
                    return true ;
                }
                listener.taskFinished( task, TaskStatus.FAILED, r.attempts, r.failureReason ) ;
                return false ;
            }
            catch( Exception e ) {
                log.error( "[{}] Error classifying {}",
                        Thread.currentThread().getName(), task, e ) ;
                listener.taskFinished( task, TaskStatus.FAILED, 0, e.getClass().getSimpleName() ) ;
                return false ;
            }
        } ;
    }

    private QTask newTask( File imgFile, String questionId ) {
        return new QTask( srcDir.getName(), srcDir, questionId,
                QTask.subjectOf( questionId ), imgFile ) ;
    }

    static String summarise( List<Topic> topics ) {
        if( topics == null || topics.isEmpty() ) return "" ;
        Topic top = topics.get( 0 ) ;
        return String.format( "%s (%d%%)", top.getTopicName(),
                Math.round( top.getMatchProbability() * 100 ) ) ;
    }

    private void markProcessed( String srcName ) {
        synchronized( PROCESSED_LIST_LOCK ) {
            try {
                Path p = processedListFile.toPath() ;
                if( Files.exists( p ) ) {
                    boolean already = Files.readAllLines( p, StandardCharsets.UTF_8 ).stream()
                            .map( String::trim ).anyMatch( srcName::equals ) ;
                    if( already ) return ;
                }
                Files.writeString( p, srcName + System.lineSeparator(), StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND ) ;
            }
            catch( Exception e ) {
                log.error( "Error appending to processed list file {}", processedListFile, e ) ;
            }
        }
    }
}
