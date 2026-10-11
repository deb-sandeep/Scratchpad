package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
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

    /** Guards {@link #queue}, {@link #inFlight} and {@link #held}. */
    private final Object      lock     = new Object() ;
    /** Tasks waiting to be dispatched by the running {@link #process}; null when it isn't running. */
    private Deque<QTask>      queue ;
    /** Tasks dispatched to the executor and not yet finished. */
    private final Set<QTask>  inFlight = new HashSet<>() ;
    /** Tasks taken out of scheduling while they are being manually associated. */
    private final Set<QTask>  held     = new HashSet<>() ;

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
    public File   getSrcDir()     { return srcDir ; }

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

        Map<String,String>  associated  = WorkspaceTopicMap.associatedSummaries( srcDir ) ;
        Map<String,Integer> aiTopIds    = WorkspaceTopicMap.topTopicIds( srcDir ) ;
        Map<String,Integer> assignedIds = QuestionTopicAssociator.assignedTopicIds( srcDir ) ;
        List<QTask> pending = new ArrayList<>() ;
        for( Map.Entry<String,List<File>> e : groups.entrySet() ) {
            QTask task = newTask( e.getKey(), e.getValue() ) ;
            String qId = task.getQuestionId() ;
            String associatedResult = associated.get( qId ) ;
            if( associatedResult != null ) {
                associatedResult += QuestionTopicAssociator.indicator( assignedIds.get( qId ), aiTopIds.get( qId ) ) ;
            }
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
        synchronized( lock ) {
            queue = new ArrayDeque<>() ;
            for( QTask t : pending ) {
                if( !classified.contains( t.getQuestionId() ) && !held.contains( t ) ) queue.add( t ) ;
            }
        }

        ExecutorCompletionService<Boolean> completion =
                new ExecutorCompletionService<>( executor ) ;
        int running = 0 ;
        try {
            while( true ) {
                QTask next ;
                while( running < maxInFlight && keepGoing.getAsBoolean() && ( next = nextTask() ) != null ) {
                    completion.submit( dispatchCallable( next ) ) ;
                    running++ ;
                }
                if( running == 0 ) {
                    // Done unless a task was put back (manual association cancelled) meanwhile.
                    synchronized( lock ) {
                        if( !keepGoing.getAsBoolean() || queue.isEmpty() ) break ;
                    }
                    continue ;
                }
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
                running-- ;
            }
        }
        finally {
            synchronized( lock ) { queue = null ; }
        }

        // Covers a source with nothing pending that was never listed.
        markProcessedIfComplete( srcDir, qIdHint, processedListFile ) ;
    }

    private QTask nextTask() {
        synchronized( lock ) {
            QTask t = queue.poll() ;
            if( t != null ) inFlight.add( t ) ;
            return t ;
        }
    }

    private Callable<Boolean> dispatchCallable( QTask task ) {
        Callable<Boolean> c = classifyCallable( task, gemmaInvoker, listener, keepGoing, processedListFile, qIdHint ) ;
        return () -> {
            try {
                return c.call() ;
            }
            finally {
                synchronized( lock ) { inFlight.remove( task ) ; }
            }
        } ;
    }

    /**
     * Take a queued task out of scheduling while it is manually associated.
     * @return false if the task has already been dispatched for classification.
     */
    public boolean hold( QTask task ) {
        synchronized( lock ) {
            if( inFlight.contains( task ) ) return false ;
            if( queue != null ) queue.remove( task ) ;
            held.add( task ) ;
            return true ;
        }
    }

    /**
     * End a {@link #hold}. A task that was not associated goes back to the
     * front of the queue.
     * @return true if a running {@link #process} will pick the task up; false
     *         if it will only be scheduled by a later run.
     */
    public boolean release( QTask task, boolean associated ) {
        synchronized( lock ) {
            if( !held.remove( task ) || associated ) return false ;
            if( queue == null ) return false ;
            queue.addFirst( task ) ;
            return true ;
        }
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
                    String summary = summarise( r.topics ) ;
                    try {
                        if( QuestionTopicAssociator.associate( task.getSrcDir(), task.getQuestionId() )
                                == QuestionTopicAssociator.Outcome.ASSOCIATED ) {
                            summary += QuestionTopicAssociator.MATCHES_AI_MARK ;
                        }
                    }
                    catch( Exception e ) {
                        // The classification itself is saved - don't fail the task over this.
                        log.error( "[{}] Topic auto-association failed", task, e ) ;
                    }
                    listener.taskFinished( task, TaskStatus.DONE, r.attempts, summary ) ;
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
