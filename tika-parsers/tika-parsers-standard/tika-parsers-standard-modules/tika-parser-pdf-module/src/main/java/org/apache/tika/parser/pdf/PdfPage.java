package org.apache.tika.parser.pdf;

import java.util.ArrayList;
import java.util.List;

public class PdfPage {

    private final int pageNumber;
    private final float width;
    private final float height;
    private final List<PdfParagraph> paragraphs = new ArrayList<>();

    PdfPage(int pageNumber, float width, float height) {
        this.pageNumber = pageNumber;
        this.width = width;
        this.height = height;
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

    public void addParagraph() {
        if (paragraphs.isEmpty() || !getLastParagraph().getTextPositions().isEmpty()) {
            PdfParagraph paragraph = new PdfParagraph();
            paragraphs.add(paragraph);
        }
    }

    public PdfParagraph getLastParagraph() {
        return paragraphs.get(paragraphs.size() - 1);
    }

    public List<PdfParagraph> getParagraphs() {
        return paragraphs;
    }

    public List<PdfParagraph> getNonEmptyParagraphs() {
        // We only add a new paragraph when previous is not empty so the only possible empty paragraph is the last
        if (getLastParagraph().getTextPositions().isEmpty()) {
            return paragraphs.subList(0, paragraphs.size() - 1);
        } else {
            return paragraphs;
        }
    }
}
