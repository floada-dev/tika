package org.apache.tika.parser.pdf;

import java.util.ArrayList;
import java.util.List;

public class PdfPage {

    private final int pageNumber;
    private final List<PdfParagraph> paragraphs = new ArrayList<>();

    PdfPage(int pageNumber) {
        this.pageNumber = pageNumber;
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

    public int getPageNumber() {
        return pageNumber;
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
