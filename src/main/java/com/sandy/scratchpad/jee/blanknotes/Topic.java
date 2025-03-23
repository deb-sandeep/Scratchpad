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
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;

import java.io.File;
import java.io.IOException;
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
public class Topic {
    
    private static final String JEE_BASE_DIR = "/Users/sandeep/Documents/StudyNotes/JEE/" ;

    private static final Logger log = Logger.getLogger( Topic.class ) ;

    private static PdfFont TOPIC_FONT;
    private static PdfFont SEC_TITLE_FONT ;
    private static PdfFont HDR_FONT ;
    private static PdfFont INDEX_FONT ;

    static {
        try {
            TOPIC_FONT = PdfFontFactory.createFont( StandardFonts.COURIER ) ;
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
        
        public Section addSection( Section childSection ) {
            this.subSections.add( childSection ) ;
            return this ;
        }

        public Section addSection( String sectionName ) {
            String subSectionId = sectionId + "." + (subSections.size()+1) ;
            Section subSection = new Section( subSectionId, sectionName ) ;
            subSections.add( subSection ) ;
            return this ;
        }

        public String toString( String indent ) {
            StringBuilder sb = new StringBuilder() ;
            sb.append( indent )
              .append( sectionId )
              .append( " - " )
              .append( sectionName ) ;
            
            for( Section subSec : subSections ) {
                sb.append( "\n" ).append( subSec.toString( indent + "  " ) );
            }
            return sb.toString() ;
        }

        public String getTitle() {
            return this.getSectionId() + " - " + this.getSectionName() ;
        }

        public void addIndex( int depth, PdfDocument pdf, Document doc ) {

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
                Topic.this.createNewPDFPage( pdf, doc ) ;
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
    private String topicGroup;
    private String topicName;
    private int    topicNumber;
    
    private List<Section> sections = new ArrayList<>() ;

    public Topic( String subject, String topicGroup,
                  int topicNumber, String topicName ) {
        this.subject = subject ;
        this.topicGroup = topicGroup;
        this.topicNumber = topicNumber;
        this.topicName = topicName;
    }
    
    public File getTopicConfigDir() {
        return new File( JEE_BASE_DIR,
                         subject + "/" +
                         StringUtils.leftPad( ""+topicNumber, 2, "0" ) + " - " + topicName +
                         "/config") ;
    }
    
    public void loadConfig() throws IOException {
        List<String> lines = FileUtils.readLines( new File( getTopicConfigDir(), "topic-map.cfg" ) ) ;
        for( String line : lines ) {
            log.debug( line + " - " + getIndentLevel( line ) ) ;
        }
    }
    
    private int getIndentLevel( String line ) {
        int numSpaces=0 ;
        for( int i=0; i<line.length(); i++ ) {
            char ch = line.charAt( i ) ;
            if( ch == ' ' ) {
                numSpaces++ ;
            }
            else { break ; }
        }
        if( numSpaces>=0 ) {
            return (numSpaces-2)/2 ;
        }
        return -1 ;
    }
    
    private void loadStaticConfig() {
        this.addSections( new String[]{
                "Definitions",
                "Remainder Theorem",
                "Factor Theorem",
                "Standard Identities",
                "Zeroes of Expression",
                "Roots of f(x) = g(x)",
                "Domain of Equation",
                "Extraneous Roots",
                "Loss of Root",
                "Graphs of polynomial fns",
                "Equations reducible to quadratic"
        } ) ;
        
        this.addSection( "Quadratic Equation", new String[]{
                "Quadratic with real coeffs",
                "Quadratic with non-real coeffs",
                "Range of quadratic",
                "Quadratic in two variables",
                "Relation between roots and coeffs of Quadratic"
        }) ;
        
        this.addSections( new String[]{
                "Symmetric Functions of Roots",
                "Common Roots",
                "Relation between root and coeffs of higher degree equations",
                "Quadratic function",
                "Rolle's Theorem",
                "Inequalities using location of roots"
        } ) ;
    }
    
    public Section addSection( String sectionName, String[] subSectionNames ) {
        
        String sectionId = topicNumber + "." + ( sections.size() + 1 ) ;
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
        return this.topicGroup + " - " +
                StringUtils.leftPad( Integer.toString( this.topicNumber ), 2, '0' )  +
                " - " + this.topicName;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder() ;
        sb.append( "Topic : " ).append( topicGroup ).append( "\n" );
        sb.append( "Subject : " ).append( subject ).append( "\n" );
        sb.append( "Chapter : " ).append( topicNumber ).append( " - " ).append( topicName ).append( "\n" );
        for( Section section : sections ) {
            sb.append( section.toString( "  " ) ).append( "\n" );
        }
        return sb.toString() ;
    }

    public void generatePDF() throws Exception {

        String relPath = StringUtils.leftPad( ""+topicNumber, 2, "0" ) + " - " + topicName + ".pdf" ;
        File outputFile = new File( getTopicConfigDir(), relPath ) ;
        log.debug( outputFile.getAbsolutePath() ) ;
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

    private void createTitlePage( PdfDocument pdf, Document doc ) {

        Paragraph chapterTitle = new Paragraph( getChapterTitle() ) ;
        chapterTitle.setFont( TOPIC_FONT ) ;
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

    private void createNewPDFPage( PdfDocument pdf, Document doc ) {

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
