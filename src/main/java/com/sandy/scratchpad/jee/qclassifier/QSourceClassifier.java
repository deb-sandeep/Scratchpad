package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.function.BooleanSupplier;

import static com.sandy.scratchpad.jee.qclassifier.FileHelper.*;

@Slf4j
public class QSourceClassifier {

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
     * Fire {@code taskDiscovered} for every question of this source (part
     * images are folded into their parent question) and return the tasks that
     * still need classification.
     */
    public List<QTask> discover() {
        Map<String,List<File>> groups = getQuestionImgGroups( srcDir, qIdHint ) ;
        if( groups == null ) {
            log.error( "No question images found for source {} and qIdHint {}", srcDir.getName(), qIdHint ) ;
            return List.of() ;
        }

        Map<String,String> associated = WorkspaceTopicMap.associatedSummaries( srcDir ) ;
        List<QTask> pending = new ArrayList<>() ;
        for( Map.Entry<String,List<File>> e : groups.entrySet() ) {
            QTask task = newTask( e.getKey(), e.getValue() ) ;
            String associatedResult = associated.get( task.getQuestionId() ) ;
            listener.taskDiscovered( task, associatedResult ) ;
            if( associatedResult == null ) pending.add( task ) ;
        }
        log.debug( "Source {} - {} of {} questions pending", srcDir.getName(), pending.size(), groups.size() ) ;
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
            completion.submit( classifyCallable( queue.poll(), gemmaInvoker, listener, keepGoing, processedListFile, qIdHint ) ) ;
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
                completion.submit( classifyCallable( queue.poll(), gemmaInvoker, listener, keepGoing, processedListFile, qIdHint ) ) ;
                inFlight++ ;
            }
        }

        // Covers a source with nothing pending that was never listed.
        markProcessedIfComplete( srcDir, qIdHint, processedListFile ) ;
    }

    /** Record the source as done only once every one of its images is classified. */
    static void markProcessedIfComplete( File srcDir, String qIdHint, File processedListFile ) {
        Map<String,List<File>> groups = getQuestionImgGroups( srcDir, qIdHint ) ;
        if( groups == null || groups.isEmpty() ) return ;
        Set<String> classified = WorkspaceTopicMap.classifiedQuestionIds( srcDir ) ;
        if( !classified.containsAll( groups.keySet() ) ) return ;
        ProcessedList.append( processedListFile, srcDir.getName() ) ;
    }

    /**
     * Callable that classifies one image, persists the result into the task's
     * source workspace map, and fires listener events. After a successful
     * classification the source is added to the processed list if that was its
     * last unclassified question. Used by both the batch pass and the UI
     * re-queue action.
     */
    static Callable<Boolean> classifyCallable( QTask task, GemmaInvoker gemmaInvoker,
                                               ClassificationListener listener, BooleanSupplier keepGoing,
                                               File processedListFile, String qIdHint ) {
        return () -> {
            listener.taskStarted( task ) ;
            try {
                GemmaInvoker.InvocationResult r = gemmaInvoker.invoke( task, keepGoing ) ;
                if( r.topics != null ) {
                    WorkspaceTopicMap.write( task.getSrcDir(), task.getQuestionId(), r.topics ) ;
                    listener.taskFinished( task, TaskStatus.DONE, r.attempts, summarise( r.topics ) ) ;
                    markProcessedIfComplete( task.getSrcDir(), qIdHint, processedListFile ) ;
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

    private QTask newTask( String questionId, List<File> imgFiles ) {
        return new QTask( srcDir.getName(), srcDir, questionId,
                QTask.subjectOf( questionId ), imgFiles ) ;
    }

    static String summarise( List<Topic> topics ) {
        if( topics == null || topics.isEmpty() ) return "" ;
        Topic top = topics.get( 0 ) ;
        return String.format( "%s (%d%%)", top.getTopicName(),
                Math.round( top.getMatchProbability() * 100 ) ) ;
    }
}
