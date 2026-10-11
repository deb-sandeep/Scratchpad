package com.sandy.scratchpad.jee.qclassifier;

/** Lifecycle state of a single question-image classification task. */
public enum TaskStatus {
    QUEUED,
    REQUEUED,
    RUNNING,
    DONE,
    FAILED,
    ASSOCIATED
}
