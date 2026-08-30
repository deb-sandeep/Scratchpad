package com.sandy.scratchpad.jee.qclassifier;

/**
 * Callbacks fired by the classification pipeline as tasks move through their
 * lifecycle. Implementations (e.g. {@link QClassifierFrame}) must be thread-safe:
 * callbacks arrive on the worker threads and on the batch thread.
 */
public interface ClassificationListener {

    /** A question image was found. {@code alreadyClassified} rows start as SKIPPED. */
    void taskDiscovered( QTask task, boolean alreadyClassified ) ;

    /** A worker picked the task up. */
    void taskStarted( QTask task ) ;

    /** The task finished (DONE / FAILED). {@code result} is a short human summary. */
    void taskFinished( QTask task, TaskStatus status, int attempts, String result ) ;

    /** Optional progress hint: the batch moved on to a new source. */
    default void batchProgress( int sourceIndex, int sourceCount, String sourceName ) {}

    /** The batch pass moved between STOPPED / RUNNING / STOPPING. */
    default void processingStateChanged( ProcessingState state ) {}

    ClassificationListener NOOP = new ClassificationListener() {
        public void taskDiscovered( QTask task, boolean alreadyClassified ) {}
        public void taskStarted( QTask task ) {}
        public void taskFinished( QTask task, TaskStatus status, int attempts, String result ) {}
    } ;
}
