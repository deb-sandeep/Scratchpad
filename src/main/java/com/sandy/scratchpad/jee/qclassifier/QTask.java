package com.sandy.scratchpad.jee.qclassifier;

import java.io.File;
import java.util.List;

/**
 * Immutable identity of one question classification task.
 * <p>
 * A question is either a single image or a set of part images
 * ({@code X(1)}, {@code X(2)}, ...) which are collated vertically before being
 * sent to the model. {@link #getQuestionId()} is always the base id (without
 * the part suffix).
 * <p>
 * Carries no mutable state - all changing state (status, timing, attempts,
 * result) lives in {@link QTaskTreeTableModel} and is only ever touched on the
 * Swing event dispatch thread, so a {@code QTask} can be shared freely between
 * the worker threads and the UI.
 */
public final class QTask {

    private final String     sourceName ;
    private final File       srcDir ;
    private final String     questionId ;
    private final String     subject ;      // "C" / "M" / "P" / "?"
    private final List<File> imgFiles ;     // one file, or the parts in ascending order

    public QTask( String sourceName, File srcDir, String questionId,
                  String subject, List<File> imgFiles ) {
        this.sourceName = sourceName ;
        this.srcDir     = srcDir ;
        this.questionId = questionId ;
        this.subject    = subject ;
        this.imgFiles   = List.copyOf( imgFiles ) ;
    }

    public String     getSourceName() { return sourceName ; }
    public File       getSrcDir()     { return srcDir ; }
    public String     getQuestionId() { return questionId ; }
    public String     getSubject()    { return subject ; }
    public List<File> getImgFiles()   { return imgFiles ; }
    public boolean    isMultiPart()   { return imgFiles.size() > 1 ; }

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
