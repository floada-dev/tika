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
package org.apache.tika.parser.xml;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;

import org.junit.jupiter.api.Test;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import org.apache.tika.TikaTest;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.sax.BodyContentHandler;

public class DcXMLParserTest extends TikaTest {

    @Test
    public void testXMLParserAsciiChars() throws Exception {
        try (InputStream input = getResourceAsStream("/test-documents/testXML.xml")) {
            Metadata metadata = new Metadata();
            ContentHandler handler = new BodyContentHandler();
            new DcXMLParser().parse(input, handler, metadata, new ParseContext());

            assertEquals("application/xml", metadata.get(Metadata.CONTENT_TYPE));
            assertEquals("Tika test document", metadata.get(TikaCoreProperties.TITLE));
            assertEquals("Rida Benjelloun", metadata.get(TikaCoreProperties.CREATOR));

            // The file contains 5 dc:subject tags, which come through as
            //  a multi-valued Tika Metadata entry in file order
            assertEquals(true, metadata.isMultiValued(TikaCoreProperties.SUBJECT));
            assertEquals(5, metadata.getValues(TikaCoreProperties.SUBJECT).length);
            assertEquals("Java", metadata.getValues(TikaCoreProperties.SUBJECT)[0]);
            assertEquals("XML", metadata.getValues(TikaCoreProperties.SUBJECT)[1]);
            assertEquals("XSLT", metadata.getValues(TikaCoreProperties.SUBJECT)[2]);
            assertEquals("JDOM", metadata.getValues(TikaCoreProperties.SUBJECT)[3]);
            assertEquals("Indexation", metadata.getValues(TikaCoreProperties.SUBJECT)[4]);

            assertEquals("Framework d\'indexation des documents XML, HTML, PDF etc..",
                    metadata.get(TikaCoreProperties.DESCRIPTION));
            assertEquals("http://www.apache.org", metadata.get(TikaCoreProperties.IDENTIFIER));
            assertEquals("test", metadata.get(TikaCoreProperties.TYPE));
            assertEquals("application/msword", metadata.get(TikaCoreProperties.FORMAT));
            assertEquals("Fr", metadata.get(TikaCoreProperties.LANGUAGE));
            assertTrue(metadata.get(TikaCoreProperties.RIGHTS).contains("testing chars"));

            String content = handler.toString();
            assertContains("Tika test document", content);

            assertEquals("2000-12-01T00:00:00.000Z", metadata.get(TikaCoreProperties.CREATED));
        }
    }

    @Test
    public void testXMLParserNonAsciiChars() throws Exception {
        try (InputStream input = getResourceAsStream("/test-documents/testXML.xml")) {
            Metadata metadata = new Metadata();
            new DcXMLParser().parse(input, new DefaultHandler(), metadata, new ParseContext());

            final String expected =
                    "Archim\u00E8de et Lius \u00E0 Ch\u00E2teauneuf testing chars en \u00E9t\u00E9";
            assertEquals(expected, metadata.get(TikaCoreProperties.RIGHTS));
        }
    }

    // TIKA-1048
    @Test
    public void testNoSpaces() throws Exception {
        String text = getXML("testXML2.xml").xml;
        assertFalse(text.contains("testSubject"));
    }
}
