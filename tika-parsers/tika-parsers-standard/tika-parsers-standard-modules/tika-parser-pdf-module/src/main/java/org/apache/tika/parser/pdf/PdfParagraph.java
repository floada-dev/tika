package org.apache.tika.parser.pdf;

import org.apache.pdfbox.text.TextPosition;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PdfParagraph {

    private final List<TextPosition> textPositions = new ArrayList<>();

    public List<TextPosition> getTextPositions() {
        return textPositions;
    }

    @Override
    public String toString() {
        return textPositions.stream()
                .map(TextPosition::getUnicode)
                .collect(Collectors.joining());
    }
}
