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
import org.apache.tika.TikaTest;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PDFParserParagraphExtractionTest extends TikaTest {

    public static Level PDFBOX_LOG_LEVEL = Level.INFO;

    @BeforeAll
    public static void setup() {
        //remember default logging level, but turn off for PDFParserTest
        PDFBOX_LOG_LEVEL = Logger.getLogger("org.apache.pdfbox").getLevel();
        Logger.getLogger("org.apache.pdfbox").setLevel(Level.OFF);
    }

    @AfterAll
    public static void tearDown() {
        //return to regular logging level
        Logger.getLogger("org.apache.pdfbox").setLevel(PDFBOX_LOG_LEVEL);
    }

    @Test
    public void testPdfParsingWithPositionsSmallDoc() throws Exception {
        CharacterPositionContentHandler contentHandler = new CharacterPositionContentHandler(new BodyContentHandler(-1));
        parse("testPDF.pdf", contentHandler);
        Map<Integer, List<TextPosition>> textPositions = contentHandler.getTextPositions();

        assertEquals(595.0, textPositions.get(1).get(0).getPageWidth());
        assertEquals(842.0, textPositions.get(1).get(0).getPageHeight());

        assertEquals(1, textPositions.size());
        assertEquals(1058, textPositions.get(1).size());
        List<TextPosition> pagePositions = textPositions.get(1);
        assertTrue(pagePositions.stream().noneMatch(tp -> tp.getUnicode().isEmpty()));
    }

    @Test
    public void testPdfParsingWithPositionsLargerDoc() throws Exception {
        CharacterPositionContentHandler contentHandler = new CharacterPositionContentHandler(new BodyContentHandler(-1));
        parse("distr_agreement_5pg.pdf", contentHandler);
        Map<Integer, List<TextPosition>> positions = contentHandler.getTextPositions();

        assertEquals(5, positions.size());
        assertEquals(3165, positions.get(1).size());
        assertEquals(2504, positions.get(5).size());
        positions.forEach((pn, tps) -> assertTrue(tps.stream().noneMatch(tp -> tp.getUnicode().isEmpty())));
    }

    @Test
    public void testPdfParsingWithParagraphPositionsSmallDoc() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("testPDF.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        assertEquals(1, pages.size());
        PdfPage page = pages.get(0);
        assertEquals(1, page.getPageNumber());
        List<PdfParagraph> paragraphs = page.getParagraphs();
        assertEquals(8, paragraphs.size());

        float width = page.getWidth();
        float height = page.getHeight();
        assertEquals(595.0, width);
        assertEquals(842.0, height);
        assertEquals(width, paragraphs.get(0).getTextPositions().get(0).getPageWidth());
        assertEquals(height, paragraphs.get(0).getTextPositions().get(0).getPageHeight());

        paragraphs.forEach(para -> {
            assertFalse(para.getTextPositions().isEmpty());
            assertTrue(para.getTextPositions().stream().noneMatch(tp -> tp.getUnicode().isEmpty()));
            assertFalse(para.toString().contains("\n"));
            assertFalse(para.toString().endsWith(" "));
        });

        assertEquals(
                "Tika - Content Analysis Toolkit",
                paragraphs.get(2).toString()
        );

        // Two long paragraphs which were previously split into multiple paragraphs each
        assertEquals(
                "Apache Tika is a toolkit for detecting and extracting metadata and structured text content from various documents using existing parser libraries.",
                paragraphs.get(3).toString()
        );
        assertEquals(
                "Apache Tika is an effort undergoing incubation at The Apache Software Foundation (ASF), sponsored by the Apache Lucene PMC. Incubation is required of all newly accepted projects until a further review indicates that the infrastructure, communications, and decision making process have stabilized in a manner consistent with other successful ASF projects. While incubation status is not necessarily a reflection of the completeness or stability of the code, it does indicate that the project has yet to be fully endorsed by the ASF.",
                paragraphs.get(4).toString()
        );
        assertEquals(
                "March 22nd, 2007: Apache Tika project started The Apache Tika project was formally started when the Tika proposal was accepted by the Apache Incubator PMC.",
                paragraphs.get(7).toString()
        );
    }

    @Test
    public void testPdfParsingWithParagraphPositionsLargerDoc() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("distr_agreement_5pg.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        assertEquals(5, pages.size());
        PdfPage pageOne = pages.get(0);
        assertEquals(1, pageOne.getPageNumber());
        List<PdfParagraph> pageOneParagraphs = pageOne.getParagraphs();
        assertEquals(21, pageOneParagraphs.size());

        pageOneParagraphs.forEach(para -> {
            assertFalse(para.getTextPositions().isEmpty());
            assertTrue(para.getTextPositions().stream().noneMatch(tp -> tp.getUnicode().isEmpty()));
            assertFalse(para.toString().contains("\n"));
            assertFalse(para.toString().endsWith(" "));
        });

        // Some long line breaking paragraphs should remain as single paragraphs
        assertEquals(
                "IN ACCORDANCE WITH ITEM 601(b) OF REGULATION S-K, CERTAIN IDENTIFIED INFORMATION (THE “CONFIDENTIALINFORMATION”) HAS BEEN EXCLUDED FROM THIS EXHIBIT BECAUSE IT IS BOTH (I) NOT MATERIAL AND (II) WOULD LIKELY CAUSE COMPETITIVE HARM IF PUBLICLY DISCLOSED. THE CONFIDENTIAL INFORMATION IS DENOTED HEREIN BY [*****].",
                pageOneParagraphs.get(1).toString()
        );
        assertEquals(
                "THIS ADDENDUM (“Addendum”) is made on the 4th day of February 2019 (“Effective Date”) between the following parties:Zebra Technologies International, LLC, with an office at 3 Overlook Point, Lincolnshire IL 60069 (“Zebra”);",
                pageOneParagraphs.get(5).toString()
        );
        // '-' could be lower in height so make sure we don't split on that
        assertEquals(
                "Zebra Technologies do Brasil - Comércio de Produtos de Informåtica Ltda., a company incorporated and organized under the laws of Brazil, withoffices at Av. Magalhäes de Castro, 4800, sala 72-A, Cidade Jardim, CEP 05676-120, Säo Paulo, sp (\"Zebra Brazil\")",
                pageOneParagraphs.get(6).toString()
        );
        assertEquals(
                "ScanSource Brazil Distribuidora de Technologias, Ltda., a ScanSource Affiliate incorporated and organized under the laws of Brazil, with officesin the City of Säo José dos Pinhais, State of Paranå, at Avenida Rui Barbosa, 2529, Modulos 11 and 12, Bairro Jardim Ipé, CEP: 83055-320, enrolledwith the Taxpayer Register (CNPJ/MF) under No. 05.607.657/0001-35 (\"ScanSource Brazil\")",
                pageOneParagraphs.get(12).toString()
        );
        assertEquals(
                "WHEREAS:(A) On February 12, 2014 the Parties entered into an agreement that was renamed, as of April 11, 2016, to: PartnerConnectTM EVM Distribution Agreement, (as amended) (\"Distribution Agreement\"), which relates to Zebra Enterprise Visibility and Mobility ('EVM\") products andservices, and which, as acknowledged by the Parties by entering into this Amendment, is in full force and effect and valid as when thisAmendment is executed;",
                pageOneParagraphs.get(16).toString()
        );
        assertEquals(
                "(D) Zebra has expanded its products portfolio by adding the product families listed in Exhibit A, that as of the Effective Date hereof arebranded Xplore or Motion Computing, thereto (“Xplore Products”); (E) Xplore, now a Zebra Affiliate, is the seller of Xplore Products;",
                pageOneParagraphs.get(19).toString()
        );

        assertEquals(7, pages.get(4).getParagraphs().size());
    }

    @Test
    @Disabled("How should we deal with this... ? Impossible to get 'line height', can just get text height")
    public void testPdfParsingWithParagraphPositionsLargeLineHeightDoc() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("msa_indemnification.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        assertEquals(28, pages.size());
        PdfPage pageOne = pages.get(0);
        assertEquals(1, pageOne.getPageNumber());
        List<PdfParagraph> pageOneParagraphs = pageOne.getParagraphs();
        assertEquals(6, pageOneParagraphs.size());

        pageOneParagraphs.forEach(para -> {
            assertFalse(para.getTextPositions().isEmpty());
            assertTrue(para.getTextPositions().stream().noneMatch(tp -> tp.getUnicode().isEmpty()));
            assertFalse(para.toString().contains("\n"));
            assertFalse(para.toString().endsWith(" "));
        });

        assertTrue(pageOneParagraphs.stream().anyMatch(p -> p.toString().equals(
                "This Master Service Agreement (the â€œMSAâ€�) is entered into between COMPANY A, LLC (â€œCOMPANY Aâ€�), a Georgia limited liability corporation, and PDX COMPANY INC______________________ (â€œClientâ€� or â€œyouâ€�)."
        )));
        assertTrue(pageOneParagraphs.stream().anyMatch(p -> p.toString().equals(
                "THIS AGREEMENT is dated for reference this 21st day of October, 2003."
        )));
        assertTrue(pageOneParagraphs.stream().anyMatch(p -> p.toString().equals(
                "14.26 Subject to Sections 11 (Warranties; Disclaimers) and 12 (Limitation of Damages) above, Cirracore shall indemnify, defend and hold Client and its employees, agents, shareholders, oﬃcers, directors, successors, End Users and assigns harmless from and against any and all claims, damages, liabilities, costs, settlements, penalties and expenses (including attorneysâ€™ fees, expertâ€™s fees and settlement costs) arising out of any."
        )));

        assertEquals(3, pages.get(3).getParagraphs().size());
    }

    @Test
    public void testPdfParsingWithParagraphPositionsConvertedDocxToPdf() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("converted_msa_indemn.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        assertEquals(20, pages.size());
        PdfPage pageOne = pages.get(0);
        assertEquals(1, pageOne.getPageNumber());
        List<PdfParagraph> pageOneParagraphs = pageOne.getParagraphs();
        assertEquals(8, pageOneParagraphs.size());

        pageOneParagraphs.forEach(para -> {
            assertFalse(para.getTextPositions().isEmpty());
            assertTrue(para.getTextPositions().stream().noneMatch(tp -> tp.getUnicode().isEmpty()));
            assertFalse(para.toString().contains("\n"));
            assertFalse(para.toString().endsWith(" "));
        });

        assertEquals(
                "INDEMNIFICATION AGREEMENT",
                pageOneParagraphs.get(0).toString()
        );
        assertEquals(
                "This Master Service Agreement (the â€œMSAâ€) is entered into between COMPANY A, LLC (â€œCOMPANY Aâ€), a Georgia limited liability corporation, and PDX COMPANY INC______________________ (â€œClientâ€ or â€œyouâ€).",
                pageOneParagraphs.get(1).toString()
        );
        assertEquals(
                "THIS AGREEMENT is dated for reference this 21st day of October, 2003.",
                pageOneParagraphs.get(2).toString()
        );
        assertEquals(
                "14.26 Subject to Sections 11 (Warranties; Disclaimers) and 12 (Limitation of Damages) above, Cirracore shall indemnify, defend and hold Client and its employees, agents, shareholders, officers, directors, successors, End Users and assigns harmless from and against any and all claims, damages, liabilities, costs, settlements, penalties and expenses (including attorneysâ€™ fees, expertâ€™s fees and settlement costs) arising out of any.",
                pageOneParagraphs.get(5).toString()
        );
        assertEquals(
                "23.6 Term: The term of this Agreement shall commence on the date first written above and shall continue until March 31, 2006, provided that Publisher shall have the option, via notice to Atari no later than February 15, 2006, to extend the term for an additional one-year period, through March 31, 2007, on the same terms and conditions (the \"TERM\"). The Term may be extended for one or more additional one (1) year periods via a mutually executed amendment to this Agreement. The three (3) month sell-off period shall commence as of the earlier expiration of this Agreement or upon notice of termination. Upon",
                pageOneParagraphs.get(7).toString()
        );

        assertEquals(7, pages.get(3).getParagraphs().size());
    }

    @Test
    public void testPdfParsingWithNormalParagraphSplitPdf() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("SLA_Pear.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        assertEquals(7, pages.size());
        assertEquals(7, pages.get(6).getPageNumber());
        PdfPage pageOne = pages.get(0);
        assertEquals(1, pageOne.getPageNumber());
        List<PdfParagraph> pageOneParagraphs = pageOne.getParagraphs();
        assertEquals(13, pageOneParagraphs.size());

        pageOneParagraphs.forEach(para -> {
            assertFalse(para.getTextPositions().isEmpty());
            assertTrue(para.getTextPositions().stream().noneMatch(tp -> tp.getUnicode().isEmpty()));
            assertFalse(para.toString().contains("\n"));
            assertFalse(para.toString().endsWith(" "));
        });

        assertEquals(
                "EXHIBIT 10.18",
                pageOneParagraphs.get(0).toString()
        );
        assertEquals(
                "SOFTWARE LICENSE AGREEMENT",
                pageOneParagraphs.get(1).toString()
        );
        assertEquals(
                "THIS SOFTWARE LICENSE AGREEMENT (the “\u200BAgreement\u200B”) is made as of December 9, 2014 (the “\u200BEffective Date\u200B”) by and between Pear Therapeutics, Inc. (“\u200BPear\u200B”), a Delaware corporation having itsprincipal place of business at 55 Temple Place, Floor 3, Boston MA 02111, and Behavioural NeurologicalApplications and Solutions Inc. (“\u200BLicensor\u200B”), having his principal place of business at 100 College Street,Suite 213, Toronto, ON M5G 1L5.",
                pageOneParagraphs.get(2).toString()
        );
        assertEquals(
                "WHEREAS Licensor owns and operates a suite of software applications called Megateam, which are software solutions for treating mental health conditions including ADHD (i.e., the Applications);",
                pageOneParagraphs.get(3).toString()
        );
        assertEquals(
                "2.1.1 \u200BCreate Combination Products\u200B: to combine and package the Licensor Products with pharmaceutical",
                pageOneParagraphs.get(12).toString()
        );

        assertEquals(10, pages.get(1).getParagraphs().size());
        assertEquals(11, pages.get(2).getParagraphs().size());
        assertEquals(8, pages.get(4).getParagraphs().size());
    }
}
