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
import org.apache.pdfbox.util.Matrix;
import org.apache.tika.sax.ContentHandlerDecorator;
import org.xml.sax.ContentHandler;

import java.util.List;

public abstract class PositionContentHandler extends ContentHandlerDecorator {

    private static final char NL = '\n';
    private static final char WS = ' ';

    PositionContentHandler(ContentHandler handler) {
        super(handler);
    }

    void nextParagraph() {

    }

    void endParagraph() {

    }

    void removeEmptyParagraph() {

    }

    void addLineSeparator() {

    }

    void addNewline() {
        addCharacter(NL, false);
    }

    void addWhitespace() {
        addCharacter(WS, true);
    }

    private void addCharacter(char character, boolean skipRepeats) {
        List<TextPosition> textPositions = getLastTextPositions();

        if (textPositions == null || textPositions.isEmpty()) {
            return;
        }

        TextPosition last = textPositions.get(textPositions.size() - 1);
        if (character == NL && " ".equals(last.getUnicode())) {
            textPositions.remove(last);
            if (textPositions.isEmpty()) {
                return;
            }

            last = textPositions.get(textPositions.size() - 1);
        }

        String stringChar = String.valueOf(character);
        if (skipRepeats && stringChar.equals(last.getUnicode())) {
            return;
        }

        TextPosition textPos = buildTextPosition(character, last);
        textPositions.add(textPos);
    }

    private static TextPosition buildTextPosition(char character, TextPosition last) {
        Matrix textMatrix = last.getTextMatrix();
        // Set start x to endX of last char
        textMatrix.setValue(2, 0, last.getEndX());

        int[] charCodes = new int[]{character};

        return new TextPosition(
                last.getRotation(),
                last.getPageWidth(),
                last.getPageHeight(),
                textMatrix,
                last.getEndX() + 0.01f,
                last.getEndY(),
                last.getHeight(),
                last.getWidth(),
                last.getWidthOfSpace(),
                String.valueOf(character),
                charCodes,
                last.getFont(),
                last.getFontSize(),
                Math.round(last.getFontSizeInPt())
        );
    }

    abstract void nextPage(float width, float height);
    abstract void addPositions(List<TextPosition> positions);
    abstract List<TextPosition> getLastTextPositions();
}
