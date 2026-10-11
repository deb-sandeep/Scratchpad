package com.sandy.scratchpad.jee.qclassifier;

import lombok.extern.slf4j.Slf4j;

import javax.swing.SwingUtilities;
import java.io.File;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
public class QImgClassifier implements QClassifierFrame.Controller {

    public static final File Q_BANK_DIR = new File( "/Users/sandeep/Documents/StudyNotes/question-bank" ) ;
    public static final String GEMMA_SERVER_URL = "http://192.168.0.149:11434/api/generate" ;
    public static final String GEMMA_MODEL = "gemma4:latest" ;

    public static final File PROCESSED_LIST_FILE =
            new File( System.getProperty( "user.home" ), ".qclassifier.processed.list" ) ;
    public static final String SOURCES_RESOURCE = "/qclassifier.sources.txt" ;

    private static final int MAX_PARALLEL_THREADS = 5 ;
    private static final String Q_ID = "" ;

    private final ExecutorService executor =
            Executors.newFixedThreadPool( MAX_PARALLEL_THREADS ) ;
    private final GemmaInvoker gemmaInvoker = new GemmaInvoker() ;
    private final ClassificationListener listener ;

    /** True while a batch thread is alive. */
    private final AtomicBoolean processing = new AtomicBoolean( false ) ;
    /** Set by {@link #stopProcessing()}; the batch thread then spawns no new tasks. */
    private volatile boolean stopRequested = false ;
    private volatile Thread batchThread ;
    private volatile List<SourceWork> plan ;
    /** Sources whose {@code process} has already finished in the current run. */
    private final Set<QSourceClassifier> finishedThisRun = ConcurrentHashMap.newKeySet() ;

    private static final class SourceWork {
        final QSourceClassifier sc ;
        final List<QTask> pending ;
        SourceWork( QSourceClassifier sc, List<QTask> pending ) {
            this.sc = sc ;
            this.pending = pending ;
        }
    }

    public QImgClassifier( ClassificationListener listener ) {
        this.listener = listener != null ? listener : ClassificationListener.NOOP ;
    }

    public static void main( String[] args ) {
        boolean headless = Arrays.asList( args ).contains( "--headless" ) ;

        if( headless ) {
            QImgClassifier app = new QImgClassifier( ClassificationListener.NOOP ) ;
            app.startProcessing() ;
            app.awaitBatch() ;
            app.shutdown() ;
            return ;
        }

        QImgClassifier[] holder = new QImgClassifier[1] ;
        try {
            SwingUtilities.invokeAndWait( () -> {
                QClassifierFrame frame = new QClassifierFrame() ;
                QImgClassifier app = new QImgClassifier( frame ) ;
                frame.setController( app ) ;
                frame.setVisible( true ) ;
                holder[0] = app ;
            } ) ;
        }
        catch( Exception e ) {
            log.error( "Could not start UI", e ) ;
            return ;
        }

        // Populate the table with the current backlog; processing waits for Start.
        new Thread( holder[0]::ensureDiscovered, "qclassifier-discover" ).start() ;
    }

    // ---- discovery ---------------------------------------------------

    private synchronized void ensureDiscovered() {
        if( plan != null ) return ;

        Set<String> processed = loadProcessedList() ;
        List<String> sources = loadSourceList() ;
        List<SourceWork> p = new ArrayList<>() ;
        for( int i = 0 ; i < sources.size() ; i++ ) {
            String srcName = sources.get( i ) ;
            listener.batchProgress( i + 1, sources.size(), srcName ) ;
            if( processed.contains( srcName ) ) continue ;
            File srcDir = FileHelper.findDir( Q_BANK_DIR, srcName ) ;
            if( srcDir == null ) {
                log.error( "Source directory {} not found", srcName ) ;
                continue ;
            }
            QSourceClassifier sc = new QSourceClassifier( srcDir, Q_ID, gemmaInvoker, executor,
                    PROCESSED_LIST_FILE, listener, this::isProcessing, MAX_PARALLEL_THREADS ) ;
            p.add( new SourceWork( sc, sc.discover() ) ) ;
        }
        plan = p ;
        int pendingCount = p.stream().mapToInt( sw -> sw.pending.size() ).sum() ;
        log.info( "Discovered {} pending tasks across {} sources", pendingCount, p.size() ) ;
    }

    private boolean isProcessing() {
        return processing.get() && !stopRequested ;
    }

    // ---- QClassifierFrame.Controller --------------------------------

    @Override
    public void startProcessing() {
        if( !processing.compareAndSet( false, true ) ) return ;
        stopRequested = false ;
        listener.processingStateChanged( ProcessingState.RUNNING ) ;
        batchThread = new Thread( this::runPlan, "qclassifier-batch" ) ;
        batchThread.start() ;
    }

    @Override
    public void stopProcessing() {
        if( processing.get() && !stopRequested ) {
            stopRequested = true ;
            listener.processingStateChanged( ProcessingState.STOPPING ) ;
            log.info( "Stop scheduled - active tasks will finish, no new tasks will start" ) ;
        }
    }

    private void runPlan() {
        try {
            ensureDiscovered() ;
            finishedThisRun.clear() ;
            List<SourceWork> p = plan ;
            for( int i = 0 ; i < p.size() ; i++ ) {
                if( !isProcessing() ) break ;
                SourceWork sw = p.get( i ) ;
                listener.batchProgress( i + 1, p.size(), sw.sc.getSourceName() ) ;
                sw.sc.process( sw.pending ) ;
                finishedThisRun.add( sw.sc ) ;
            }
        }
        finally {
            stopRequested = false ;
            processing.set( false ) ;
            listener.processingStateChanged( ProcessingState.STOPPED ) ;
            log.info( "Processing stopped" ) ;
        }
    }

    public void awaitBatch() {
        Thread t = batchThread ;
        if( t != null ) {
            try { t.join() ; }
            catch( InterruptedException e ) { Thread.currentThread().interrupt() ; }
        }
    }

    /**
     * Re-sync the processed list with the topic maps (see {@link ProcessedListRepair}).
     * Refused while a batch is running. Sources dropped from the list are
     * discovered and appended to the plan so their unassociated questions show
     * up and are picked up by the next Start.
     */
    @Override
    public synchronized ProcessedListRepair.Report repairProcessedList( ProcessedListRepair.ProgressListener progress ) {
        if( processing.get() ) throw new IllegalStateException( "Stop processing before repairing the processed list" ) ;
        ensureDiscovered() ;
        ProcessedListRepair.Report report = ProcessedListRepair.run(
                Q_BANK_DIR, loadSourceList(), PROCESSED_LIST_FILE, Q_ID, progress ) ;

        Set<String> planned = plan.stream().map( sw -> sw.sc.getSourceName() ).collect( Collectors.toSet() ) ;
        List<SourceWork> p = new ArrayList<>( plan ) ;
        for( String srcName : report.getRemoved() ) {
            if( planned.contains( srcName ) ) continue ;
            File srcDir = FileHelper.findDir( Q_BANK_DIR, srcName ) ;
            if( srcDir == null ) continue ;
            QSourceClassifier sc = new QSourceClassifier( srcDir, Q_ID, gemmaInvoker, executor,
                    PROCESSED_LIST_FILE, listener, this::isProcessing, MAX_PARALLEL_THREADS ) ;
            p.add( new SourceWork( sc, sc.discover() ) ) ;
        }
        plan = p ;
        return report ;
    }

    @Override
    public void reprocess( QTask task ) {
        log.debug( "Re-queueing {}", task ) ;
        executor.submit( QSourceClassifier.classifyCallable( task, gemmaInvoker, listener, () -> true,
                PROCESSED_LIST_FILE, Q_ID ) ) ;
    }

    @Override
    public boolean holdForManualAssociation( QTask task ) {
        QSourceClassifier sc = sourceOf( task ) ;
        return sc == null || sc.hold( task ) ;
    }

    @Override
    public void releaseFromManualAssociation( QTask task, boolean associated ) {
        QSourceClassifier sc = sourceOf( task ) ;
        if( sc == null || sc.release( task, associated ) || associated ) return ;
        // Its source already ran in this batch, so nothing would pick it up until the next Start.
        if( isProcessing() && finishedThisRun.contains( sc ) ) {
            log.debug( "Scheduling {} after cancelled manual association", task ) ;
            executor.submit( QSourceClassifier.classifyCallable( task, gemmaInvoker, listener, this::isProcessing,
                    PROCESSED_LIST_FILE, Q_ID ) ) ;
        }
    }

    private QSourceClassifier sourceOf( QTask task ) {
        List<SourceWork> p = plan ;
        if( p == null ) return null ;
        for( SourceWork sw : p ) {
            if( sw.sc.getSrcDir().equals( task.getSrcDir() ) ) return sw.sc ;
        }
        return null ;
    }

    @Override
    public QuestionTopicAssociator.Outcome manualAssociate( QTask task, Topic topic ) {
        log.debug( "Manually associating {} with topic {}", task, topic.getId() ) ;
        WorkspaceTopicMap.writeManual( task.getSrcDir(), task.getQuestionId(), topic ) ;
        QuestionTopicAssociator.Outcome outcome =
                QuestionTopicAssociator.associateManual( task.getSrcDir(), task.getQuestionId(), topic ) ;
        QSourceClassifier.markProcessedIfComplete( task.getSrcDir(), Q_ID, PROCESSED_LIST_FILE ) ;
        return outcome ;
    }

    @Override
    public void shutdown() {
        log.debug( "Shutting down classifier executor" ) ;
        stopRequested = true ;
        executor.shutdownNow() ;
        try {
            executor.awaitTermination( 10, TimeUnit.SECONDS ) ;
        }
        catch( InterruptedException e ) {
            Thread.currentThread().interrupt() ;
        }
    }

    // ---- config loading --------------------------------------------

    private Set<String> loadProcessedList() {
        return ProcessedList.load( PROCESSED_LIST_FILE ) ;
    }

    private List<String> loadSourceList() {
        try( InputStream is = getClass().getResourceAsStream( SOURCES_RESOURCE ) ) {
            if( is == null ) {
                log.error( "Sources resource {} not found", SOURCES_RESOURCE ) ;
                return List.of() ;
            }
            String content = new String( is.readAllBytes(), StandardCharsets.UTF_8 ) ;
            return content.lines()
                    .map( String::trim )
                    .filter( s -> !s.isEmpty() && !s.startsWith( "#" ) )
                    .collect( Collectors.toList() ) ;
        }
        catch( Exception e ) {
            log.error( "Error loading sources resource {}", SOURCES_RESOURCE, e ) ;
            return List.of() ;
        }
    }
}
