package com.sandy.scratchpad.jee.blanknotes;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.canvas.draw.DottedLine;
import com.itextpdf.layout.element.AreaBreak;
import com.itextpdf.layout.element.LineSeparator;
import lombok.Data;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.VerticalAlignment;
import org.apache.log4j.Logger;

@Data
public class Chapter {

    private static final Logger log = Logger.getLogger( Chapter.class ) ;

    private static PdfFont CHAPTER_FONT ;
    private static PdfFont SEC_TITLE_FONT ;
    private static PdfFont HDR_FONT ;
    private static PdfFont INDEX_FONT ;

    static {
        try {
            CHAPTER_FONT = PdfFontFactory.createFont( StandardFonts.COURIER ) ;
            SEC_TITLE_FONT = PdfFontFactory.createFont( StandardFonts.COURIER ) ;
            HDR_FONT = PdfFontFactory.createFont( StandardFonts.HELVETICA_OBLIQUE ) ;
            INDEX_FONT = PdfFontFactory.createFont( StandardFonts.TIMES_ROMAN ) ;
        }
        catch( Exception ignore ) {}
    }

    @Data
    public class Section {

        private String sectionId ;
        private String sectionName ;
        private List<Section> subSections = new ArrayList<>() ;

        private Section( String sectionId, String sectionName ) {
            this.sectionId = sectionId ;
            this.sectionName = sectionName ;
        }

        public Section addSection( String sectionName ) {
            String subSectionId = sectionId + "." + (subSections.size()+1) ;
            Section subSection = new Section( subSectionId, sectionName ) ;
            subSections.add( subSection ) ;
            return subSection ;
        }

        public String toString( String indent ) {
            StringBuilder sb = new StringBuilder() ;
            sb.append( indent + sectionId + " - " + sectionName ) ;
            for( Section subSec : subSections ) {
                sb.append( "\n" + subSec.toString( indent + "  " ) ) ;
            }
            return sb.toString() ;
        }

        public String getTitle() {
            return this.getSectionId() + " - " + this.getSectionName() ;
        }

        public void addIndex( int depth, PdfDocument pdf, Document doc ) throws Exception {

            int fontSize = depth == 1 ? 12 : ( depth == 2 ? 10 : 8 ) ;
            int indent = 10 * depth ;

            Paragraph indexPara = new Paragraph( getTitle() ) ;
            indexPara.setPaddingLeft( indent ) ;
            indexPara.setFont( INDEX_FONT ) ;
            indexPara.setFontSize( fontSize ) ;
            indexPara.setTextAlignment( TextAlignment.LEFT ) ;
            doc.add( indexPara ) ;

            if( !subSections.isEmpty() ) {
                for( Section sub : subSections ) {
                    sub.addIndex( depth+1, pdf, doc ) ;
                }
            }
        }

        public void createSectionPages( int depth, PdfDocument pdf, Document doc,
                                        boolean createNewPage ) throws  Exception {

            int fontSize = depth == 1 ? 20 : ( depth == 2 ? 16 : 12 ) ;

            if( createNewPage ) {
                Chapter.this.createNewPDFPage( pdf, doc ) ;
                doc.add( new AreaBreak() ) ;
            }

            Paragraph title = new Paragraph( getTitle() ) ;
            title.setFont( SEC_TITLE_FONT ) ;
            title.setFontSize( fontSize ) ;
            title.setTextAlignment( TextAlignment.LEFT ) ;
            doc.add( title ) ;

            if( !subSections.isEmpty() ) {
                for( int i=0; i<subSections.size(); i++ ) {
                    subSections.get(i).createSectionPages( depth+1, pdf, doc, i!=0 ) ;
                }
            }
        }
    }

    private String subject ;
    private String topic ;
    private String chapterName ;
    private int chapterNumber ;
    private List<Section> sections = new ArrayList<>() ;

    public Chapter( String subject, String topic,
                    int chapterNumber, String chapterName ) {
        this.subject = subject ;
        this.topic = topic ;
        this.chapterNumber = chapterNumber ;
        this.chapterName = chapterName ;
    }

    public Section addSection( String sectionName, String[] subSectionNames ) {
        String sectionId = chapterNumber + "." + ( sections.size() + 1 ) ;
        Section section = new Section( sectionId, sectionName ) ;
        sections.add( section ) ;

        if( subSectionNames != null ) {
            for( String sub : subSectionNames ) {
                section.addSection( sub ) ;
            }
        }
        return section ;
    }

    public Section addSection( String sectionName ) {
        return this.addSection( sectionName, null ) ;
    }

    public void addSections( String[] sectionNames ) {
        for( String sec : sectionNames ) {
            this.addSection( sec, null ) ;
        }
    }

    public String getChapterTitle() {
        return this.topic + " - " +
                StringUtils.leftPad( Integer.toString( this.chapterNumber ), 2, '0' )  +
                " - " + this.chapterName ;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder() ;
        sb.append( "Topic : " + topic + "\n" ) ;
        sb.append( "Subject : " + subject + "\n" ) ;
        sb.append( "Chapter : " + chapterNumber + " - " + chapterName + "\n" ) ;
        for( Section section : sections ) {
            sb.append( section.toString( "  " ) + "\n" ) ;
        }
        return sb.toString() ;
    }

    public void generatePDF( File dir ) throws Exception {

        String relPath = this.subject + "/" + this.topic + "/" +
                         getChapterTitle() + ".pdf" ;
        File outputFile = new File( dir, relPath ) ;
        outputFile.getParentFile().mkdirs() ;

        PdfWriter pdfWriter = new PdfWriter( outputFile ) ;
        PdfDocument pdf = new PdfDocument( pdfWriter ) ;
        Document doc = new Document( pdf, PageSize.A4 ) ;

        addPDFContent( pdf, doc ) ;
        doc.close() ;
    }

    private void addPDFContent( PdfDocument pdf, Document doc ) throws Exception {

        createNewPDFPage( pdf, doc ) ;
        createTitlePage( pdf, doc ) ;
        for( Section section : sections ) {
            section.createSectionPages( 1, pdf, doc, true ) ;
        }
    }

    private void createTitlePage( PdfDocument pdf, Document doc ) throws Exception {

        Paragraph chapterTitle = new Paragraph( getChapterTitle() ) ;
        chapterTitle.setFont( CHAPTER_FONT ) ;
        chapterTitle.setFontSize( 22 ) ;
        chapterTitle.setFontColor( ColorConstants.BLUE ) ;
        chapterTitle.setTextAlignment( TextAlignment.CENTER ) ;
        doc.add( chapterTitle ) ;

        LineSeparator sep = new LineSeparator( new DottedLine() ) ;
        doc.add( sep ) ;

        for( Section section : sections ) {
            section.addIndex( 1, pdf, doc ) ;
        }
    }

    private void createNewPDFPage( PdfDocument pdf, Document doc ) throws Exception {

        PdfPage page = pdf.addNewPage() ;

        Paragraph header = new Paragraph( getChapterTitle() ) ;
        header.setFont( HDR_FONT ) ;
        header.setFontSize( 14 ) ;
        header.setFontColor( ColorConstants.LIGHT_GRAY ) ;

        Rectangle pageSize = pdf.getLastPage().getPageSize() ;
        float x1 = pageSize.getWidth() / 2;
        float y1 = pageSize.getTop() - 25;
        doc.showTextAligned( header, x1, y1, pdf.getPageNumber( pdf.getLastPage() ),
                             TextAlignment.CENTER, VerticalAlignment.BOTTOM, 0 ) ;

        PdfCanvas canvas = new PdfCanvas( page ) ;
        Color lastColor = canvas.getGraphicsState().getStrokeColor();
        canvas.setColor( ColorConstants.LIGHT_GRAY, true ) ;
        for (float x = 0; x < pageSize.getWidth(); ) {
            for (float y = 0; y < pageSize.getHeight(); ) {
                canvas.circle(x, y, 0.5f);
                y += 15f;
            }
            x += 15f;
        }
        canvas.fill();
        canvas.setColor( lastColor, true ) ;
    }
}
