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
        if (positions.stream().allMatch(p -> p.getUnicode().isBlank())) {
           return;
        }

        List<PdfParagraph> paragraphs = getLastPage().getParagraphs();
        List<TextPosition> lastTextPositions = getLastTextPositions();

        if (paragraphs.size() > 1 && lastTextPositions.isEmpty()) {
            PdfParagraph lastNonEmptyParagraph = paragraphs.get(paragraphs.size() - 2);
            List<TextPosition> lastNonEmptyParagraphPositions = lastNonEmptyParagraph.getTextPositions();
            float lastBottomY = lastNonEmptyParagraphPositions.get(lastNonEmptyParagraphPositions.size() - 1).getY();
            TextPosition nextPosition = positions.get(0);
            float nextTopY = nextPosition.getY() - nextPosition.getHeight();

            // Force non-paragraph break if the perceived paragraphs are too close to each other.
            // Almost certainly just a line break with larger spacing. Min space for paragraph break: 3 x height of text
            if (Math.abs(nextTopY - lastBottomY) < 3 * nextPosition.getHeight()) {
                paragraphs.remove(paragraphs.size() - 1);
                addWhitespace();
                lastNonEmptyParagraph.getTextPositions().addAll(positions);
            } else {
                lastTextPositions.addAll(positions);
            }
        } else {
            lastTextPositions.addAll(positions);
        }
    }

    private PdfPage getLastPage() {
        return pages.get(pages.size() - 1);
    }
}
