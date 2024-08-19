package org.apache.tika.parser.pdf;

import java.util.List;

public class PdfPage {

    private final int pageNumber;
    private final float width;
    private final float height;
    private final List<PdfParagraph> paragraphs;

    PdfPage(int pageNumber, float width, float height, List<PdfParagraph> paragraphs) {
        this.pageNumber = pageNumber;
        this.width = width;
        this.height = height;
        this.paragraphs = paragraphs;
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public float getWidth() {
        return width;
    }

    public float getHeight() {
        return height;
    }

    public List<PdfParagraph> getParagraphs() {
        return paragraphs;
    }
}
