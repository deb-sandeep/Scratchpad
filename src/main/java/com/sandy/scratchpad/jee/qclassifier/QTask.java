package com.sandy.scratchpad.jee.qclassifier;

import java.io.File;

/**
 * Immutable identity of one question-image classification task.
 * <p>
 * Carries no mutable state - all changing state (status, timing, attempts,
 * result) lives in {@link QTaskTableModel} and is only ever touched on the Swing
 * event dispatch thread, so a {@code QTask} can be shared freely between the
 * worker threads and the UI.
 */
public final class QTask {

    private final String sourceName ;
    private final File   srcDir ;
    private final String questionId ;
    private final String subject ;      // "C" / "M" / "P" / "?"
    private final File   imgFile ;

    public QTask( String sourceName, File srcDir, String questionId,
                  String subject, File imgFile ) {
        this.sourceName = sourceName ;
        this.srcDir     = srcDir ;
        this.questionId = questionId ;
        this.subject    = subject ;
        this.imgFile    = imgFile ;
    }

    public String getSourceName() { return sourceName ; }
    public File   getSrcDir()     { return srcDir ; }
    public String getQuestionId() { return questionId ; }
    public String getSubject()    { return subject ; }
    public File   getImgFile()    { return imgFile ; }

    public static String subjectOf( String questionId ) {
        if( questionId.startsWith( "C_" ) ) return "C" ;
        if( questionId.startsWith( "M_" ) ) return "M" ;
        if( questionId.startsWith( "P_" ) ) return "P" ;
        return "?" ;
    }

    @Override
    public String toString() {
        return sourceName + "/" + questionId ;
    }
}
