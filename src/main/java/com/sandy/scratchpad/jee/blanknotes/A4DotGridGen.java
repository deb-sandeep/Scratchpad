package com.sandy.scratchpad.jee.blanknotes;

import com.itextpdf.kernel.colors.Color;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.layout.Document;

import java.io.File;

public class A4DotGridGen {

    public static void main(String[] args) throws  Exception {

        File outputFile = new File( "/Users/sandeep/temp/A4DotGrid.pdf" ) ;
        A4DotGridGen gen = new A4DotGridGen() ;
        gen.generatePDF( outputFile ) ;
    }

    public void generatePDF( File outputFile ) throws Exception {

        PdfWriter pdfWriter = new PdfWriter( outputFile ) ;
        PdfDocument pdf = new PdfDocument( pdfWriter ) ;
        Document doc = new Document( pdf, PageSize.A4 ) ;
        createDotGridPage( pdf, doc ) ;
        doc.close() ;
    }

    private void createDotGridPage( PdfDocument pdf, Document doc ) throws Exception {

        PdfPage page = pdf.addNewPage() ;
        Rectangle pageSize = pdf.getLastPage().getPageSize() ;
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
