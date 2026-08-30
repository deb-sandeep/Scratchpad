package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;

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

    public QSourceClassifier( File srcDir, String qIdHint, GemmaInvoker gemmaInvoker,
                              ExecutorService executor, File processedListFile,
                              ClassificationListener listener ) {
        this.srcDir = srcDir ;
        this.qIdHint = qIdHint ;
        this.gemmaInvoker = gemmaInvoker ;
        this.executor = executor ;
        this.processedListFile = processedListFile ;
        this.listener = listener != null ? listener : ClassificationListener.NOOP ;
    }

    public void process() {
        log.debug( "Processing source - {}", srcDir.getName() ) ;
        log.debug( "----------------------------------------" ) ;

        File[] qImgFiles = getQuestionImgFiles( srcDir, qIdHint ) ;
        if( qImgFiles == null ) {
            log.error( "No question images found for source {} and qIdHint {}", srcDir.getName(), qIdHint ) ;
            return ;
        }

        Set<String> alreadyClassified = WorkspaceTopicMap.classifiedQuestionIds( srcDir ) ;
        int total = qImgFiles.length ;

        List<QTask> toProcess = new ArrayList<>() ;
        for( File f : qImgFiles ) {
            String questionId = extractQuestionId( f ) ;
            QTask task = newTask( f, questionId ) ;
            boolean done = alreadyClassified.contains( questionId ) ;
            listener.taskDiscovered( task, done ) ;
            if( !done ) toProcess.add( task ) ;
        }
        log.debug( "Source {} - {} of {} images remaining to process",
                srcDir.getName(), toProcess.size(), total ) ;

        boolean successfullyProcessed = true ;
        if( !toProcess.isEmpty() ) {
            ExecutorCompletionService<Boolean> completion =
                    new ExecutorCompletionService<>( executor ) ;
            for( QTask task : toProcess ) {
                completion.submit( classifyCallable( task, gemmaInvoker, listener ) ) ;
            }

            int totalToProcess = toProcess.size() ;
            for( int done = 1 ; done <= totalToProcess ; done++ ) {
                try {
                    boolean ok = completion.take().get() ;
                    if( !ok ) successfullyProcessed = false ;
                }
                catch( Exception e ) {
                    log.error( "Error awaiting image result for source {}", srcDir.getName(), e ) ;
                    successfullyProcessed = false ;
                    if( e instanceof InterruptedException ) {
                        Thread.currentThread().interrupt() ;
                        break ;
                    }
                }
            }
        }

        if( successfullyProcessed ) {
            markProcessed( srcDir.getName() ) ;
        }
    }

    /**
     * Callable that classifies one image, persists the result into the task's
     * source workspace map, and fires listener events. Used by both the batch
     * pass and the UI re-queue action.
     */
    static Callable<Boolean> classifyCallable( QTask task, GemmaInvoker gemmaInvoker,
                                               ClassificationListener listener ) {
        return () -> {
            listener.taskStarted( task ) ;
            try {
                GemmaInvoker.InvocationResult r = gemmaInvoker.invoke( task.getImgFile() ) ;
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
                Files.writeString( processedListFile.toPath(), srcName + System.lineSeparator(),
                        StandardCharsets.UTF_8,
                        StandardOpenOption.CREATE, StandardOpenOption.APPEND ) ;
            }
            catch( Exception e ) {
                log.error( "Error appending to processed list file {}", processedListFile, e ) ;
            }
        }
    }
}
