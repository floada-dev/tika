/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.tika.parser.pdf;

import org.apache.pdfbox.text.TextPosition;
import org.xml.sax.ContentHandler;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ParagraphAwarePositionContentHandler extends PositionContentHandler {


    private final List<PdfPage> pages = new ArrayList<>();
    private final List<TextLine> pageTextLines = new ArrayList<>();

    public ParagraphAwarePositionContentHandler(ContentHandler contentHandler) {
        super(contentHandler);
    }

    public List<PdfPage> getPages() {
        return pages;
    }

    @Override
    void endPage(float width, float height) {
        float minLineSpacing = findMinLineSpacing();
        List<PdfParagraph> paragraphs = buildParagraphs(minLineSpacing);

        int pageNumber = pages.size() + 1;
        PdfPage pdfPage = new PdfPage(pageNumber, width, height, paragraphs);

        pages.add(pdfPage);
        pageTextLines.clear();
    }

    @Override
    List<TextPosition> getLastTextPositions() {
        if (pageTextLines.isEmpty()) {
            return List.of();
        }

        return pageTextLines.get(pageTextLines.size() - 1).textPositions;
    }

    @Override
    void addPositions(List<TextPosition> positions) {
        // Trim whitespace from start and end string (last position)
        ListIterator<TextPosition> positionStartIterator = positions.listIterator();
        while (positionStartIterator.hasNext() && isOnlyWhitespace(positionStartIterator.next().getUnicode())) {
            positionStartIterator.remove();
        }
        ListIterator<TextPosition> positionEndIterator = positions.listIterator(positions.size());
        while (positionEndIterator.hasPrevious() && isOnlyWhitespace(positionEndIterator.previous().getUnicode())) {
            positionEndIterator.remove();
        }

        // Do not add blank lines
        if (positions.stream().allMatch(p -> isOnlyWhitespace(p.getUnicode()))) {
            return;
        }

        float lineTopYSum = 0.0f;
        float lineBottomYSum = 0.0f;
        for (TextPosition position : positions) {
            lineTopYSum += position.getY() - position.getHeight();
            lineBottomYSum += position.getY();
        }
        float avgLineTopY = lineTopYSum / positions.size();
        float avgLineBottomY = lineBottomYSum / positions.size();

        pageTextLines.add(new TextLine(avgLineTopY, avgLineBottomY, positions));
    }

    private List<PdfParagraph> buildParagraphs(float minLineSpacing) {
        if (pageTextLines.isEmpty()) {
            return new ArrayList<>();
        } else if (pageTextLines.size() == 1) {
            return new ArrayList<>(Collections.singletonList(new PdfParagraph(pageTextLines.get(0).textPositions)));
        }

        List<PdfParagraph> paragraphs = new ArrayList<>();
        List<TextPosition> paragraphTextPositions = new ArrayList<>(pageTextLines.get(0).textPositions);

        for (int i = 1; i < pageTextLines.size(); i++) {
            TextLine currLine = pageTextLines.get(i);
            TextLine lastLine = pageTextLines.get(i - 1);
            if (isParagraphSplit(currLine, lastLine, minLineSpacing)) {
                paragraphs.add(new PdfParagraph(new ArrayList<>(paragraphTextPositions)));
                paragraphTextPositions.clear();
            }

            addWhitespaceTo(paragraphTextPositions);
            paragraphTextPositions.addAll(currLine.textPositions);
        }

        paragraphs.add(new PdfParagraph(new ArrayList<>(paragraphTextPositions)));

        return paragraphs;
    }

    /**
     * 1. Use absolute spacing to handle incorrectly ordered text lines where the spacing is huge, but negative.
     * 2. Require 1.5 * minLineSpacing with 10% leeway.
     * 3. Require spacing to be greater than line height to handle same line being incorrectly split into multiple by tika.
     */
    private boolean isParagraphSplit(TextLine currLine, TextLine lastLine, float minLineSpacing) {
        float spacing = Math.abs(currLine.topY - lastLine.bottomY);
        return spacing > 1.65 * minLineSpacing && spacing > currLine.height();
    }

    private float findMinLineSpacing() {
        if (pageTextLines.size() < 2) return 0f;

        List<Float> lineSpacings = new ArrayList<>();
        for (int i = 0; i < pageTextLines.size() - 1; i++) {
            TextLine curr = pageTextLines.get(i);
            TextLine next = pageTextLines.get(i + 1);
            float lineSpacing = next.topY - curr.bottomY;
            // Negative line spacing means we found "two lines on the same Y coordinates"
            if (lineSpacing > 0) {
                lineSpacings.add(lineSpacing);
            }
        }
        lineSpacings.sort(Comparator.comparing(Function.identity(), Float::compareTo));

        // Only negative line spacings, see comment above.
        if (lineSpacings.isEmpty()) {
            return 0f;
        }

        // Remove bottom 15% of smallest line spacing to adjust for things like scanned signatures where line boundaries might be broken
        return lineSpacings.get(Math.round(lineSpacings.size() * 0.15f));
    }

    static class TextLine {
        private final float topY;
        private final float bottomY;
        private final List<TextPosition> textPositions;

        TextLine(float topY, float bottomY, List<TextPosition> textPositions) {
            this.topY = topY;
            this.bottomY = bottomY;
            this.textPositions = textPositions;
        }

        float height() {
            return this.bottomY - this.topY;
        }

        @Override
        public String toString() {
            return textPositions.stream().map(TextPosition::getUnicode).collect(Collectors.joining(""));
        }
    }
}
