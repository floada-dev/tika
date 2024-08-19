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
import java.util.List;
import java.util.ListIterator;
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
        return pageTextLines.get(pageTextLines.size() - 1).textPositions;
    }

    @Override
    void addPositions(List<TextPosition> positions) {
        // Trim whitespace from start and end string (last position)
        ListIterator<TextPosition> positionStartIterator = positions.listIterator();
        while (positionStartIterator.hasNext() && positionStartIterator.next().getUnicode().isBlank()) {
            positionStartIterator.remove();
        }
        ListIterator<TextPosition> positionEndIterator = positions.listIterator(positions.size());
        while (positionEndIterator.hasPrevious() && positionEndIterator.previous().getUnicode().isBlank()) {
            positionEndIterator.remove();
        }

        // Do not add blank lines
        if (positions.stream().allMatch(p -> p.getUnicode().isBlank())) {
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
            // 1.5 * minLineSpacing with 10% leeway
            if (Math.abs(currLine.topY - lastLine.bottomY) > 1.65 * minLineSpacing) {
                paragraphs.add(new PdfParagraph(new ArrayList<>(paragraphTextPositions)));
                paragraphTextPositions.clear();
            }

            addWhitespaceTo(paragraphTextPositions);
            paragraphTextPositions.addAll(currLine.textPositions);
        }

        paragraphs.add(new PdfParagraph(new ArrayList<>(paragraphTextPositions)));

        return paragraphs;
    }

    private float findMinLineSpacing() {
        float minLineSpacing = Float.MAX_VALUE;
        for (int i = 0; i < pageTextLines.size() - 1; i++) {
            TextLine curr = pageTextLines.get(i);
            TextLine next = pageTextLines.get(i + 1);
            float lineSpacing = next.topY - curr.bottomY;
            // Negative line spacing means we found "two lines on the same Y coordinates"
            if (lineSpacing > 0) {
                minLineSpacing = Math.min(minLineSpacing, lineSpacing);
            }
        }
        return minLineSpacing;
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

        @Override
        public String toString() {
            return textPositions.stream().map(TextPosition::getUnicode).collect(Collectors.joining(""));
        }
    }
}
