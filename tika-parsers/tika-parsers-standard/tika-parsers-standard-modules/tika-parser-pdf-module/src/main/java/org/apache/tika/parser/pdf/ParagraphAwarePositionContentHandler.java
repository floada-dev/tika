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
import java.util.List;

public class ParagraphAwarePositionContentHandler extends PositionContentHandler {

    private static final float MIN_LINE_HEIGHT = 4.0f;

    private final List<PdfPage> pages = new ArrayList<>();

    public ParagraphAwarePositionContentHandler(ContentHandler contentHandler) {
        super(contentHandler);
    }

    public List<PdfPage> getPages() {
        return pages;
    }

    @Override
    void nextPage(float width, float height) {
        int pageNumber = pages.size();
        PdfPage page = new PdfPage(++pageNumber, width, height);
        pages.add(page);
    }

    @Override
    void nextParagraph() {
        getLastPage().addParagraph();
    }

    @Override
    void removeEmptyParagraph() {
        PdfPage lastPage = getLastPage();
        List<TextPosition> lastParagraphTextPositions = lastPage.getLastParagraph().getTextPositions();
        if (lastParagraphTextPositions.isEmpty()) {
            List<PdfParagraph> lastPageParagraphs = lastPage.getParagraphs();
            lastPageParagraphs.remove(lastPageParagraphs.size() - 1);
        }
    }

    @Override
    List<TextPosition> getLastTextPositions() {
        return getLastPage().getLastParagraph().getTextPositions();
    }

    @Override
    void addPositions(List<TextPosition> positions) {
        // Trim string (last position)
        if (positions.get(positions.size() - 1).getUnicode().isBlank()) {
            positions.remove(positions.size() - 1);
        }

        if (positions.stream().allMatch(p -> p.getUnicode().isBlank())) {
            return;
        }

        List<PdfParagraph> paragraphs = getLastPage().getParagraphs();
        List<TextPosition> lastTextPositions = getLastTextPositions();

        // First text positions of a new paragraph (as identified by tika
        if (paragraphs.size() > 1 && lastTextPositions.isEmpty()) {
            boolean mergedAndAdded = maybeMergeParagraphs(paragraphs, positions);
            if (mergedAndAdded) {
                return;
            }
        } else if (!paragraphs.isEmpty() && !lastTextPositions.isEmpty()) {
            boolean splitAndAdded = maybeSplitParagraph(lastTextPositions, positions);
            if (splitAndAdded) {
                return;
            }
        }

        lastTextPositions.addAll(positions);
    }

    private boolean maybeMergeParagraphs(List<PdfParagraph> paragraphs, List<TextPosition> currPositions) {
        PdfParagraph lastNonEmptyParagraph = paragraphs.get(paragraphs.size() - 2);
        List<TextPosition> lastNonEmptyParagraphPositions = lastNonEmptyParagraph.getTextPositions();
        TextPosition lastPosition = lastNonEmptyParagraphPositions.get(lastNonEmptyParagraphPositions.size() - 1);

        // Force paragraph merge if the perceived paragraphs are too close to each other.
        // Almost certainly just a line break with maybe larger spacing. Min space for paragraph break: 2 x height of text
        if (!lastAndCurrDistanceExceedThreshold(lastPosition, currPositions.get(0))) {
            paragraphs.remove(paragraphs.size() - 1);
            addWhitespace();
            lastNonEmptyParagraph.getTextPositions().addAll(currPositions);
            return true;
        }

        return false;
    }

    private boolean maybeSplitParagraph(List<TextPosition> lastPositions, List<TextPosition> currPositions) {
        TextPosition lastPosition = lastPositions.get(lastPositions.size() - 1);

        // Force paragraph split when lines are too far from each other.
        if (lastAndCurrDistanceExceedThreshold(lastPosition, currPositions.get(0))) {
            nextParagraph();
            addPositions(currPositions);
            return true;
        }

        return false;
    }

    private boolean lastAndCurrDistanceExceedThreshold(TextPosition lastPosition, TextPosition currPosition) {
        float lastBottomY = lastPosition.getY();
        float currTopY = currPosition.getY() - currPosition.getHeight();
        float charHeight = Math.max(Math.max(currPosition.getHeight(), lastPosition.getHeight()), MIN_LINE_HEIGHT);
        return Math.abs(currTopY - lastBottomY) > 2 * charHeight;
    }

    private PdfPage getLastPage() {
        return pages.get(pages.size() - 1);
    }
}
