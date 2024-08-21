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
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PDFParserCharacterAndParagraphExtractionTest extends TikaTest {

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

        basePositionsAssert(pages);

        assertEquals("Apache Tika - Apache Tika http://incubator.apache.org/tika/", paragraphs.get(0).toString());
        assertEquals("1 of 1 15.9.2007 11:02", paragraphs.get(1).toString());
        assertEquals("Tika - Content Analysis Toolkit", paragraphs.get(2).toString());
        // Two long paragraphs which were previously split into multiple paragraphs each
        assertEquals("Apache Tika is a toolkit for detecting and extracting metadata and structured text content from various documents using existing parser libraries.", paragraphs.get(3).toString());
        assertEquals("Apache Tika is an effort undergoing incubation at The Apache Software Foundation (ASF), sponsored by the Apache Lucene PMC. Incubation is required of all newly accepted projects until a further review indicates that the infrastructure, communications, and decision making process have stabilized in a manner consistent with other successful ASF projects. While incubation status is not necessarily a reflection of the completeness or stability of the code, it does indicate that the project has yet to be fully endorsed by the ASF.", paragraphs.get(4).toString());
        assertEquals("See the Apache Tika Incubation Status page for the current incubation status.", paragraphs.get(5).toString());
        assertEquals("Latest News", paragraphs.get(6).toString());
        assertEquals("March 22nd, 2007: Apache Tika project started The Apache Tika project was formally started when the Tika proposal was accepted by the Apache Incubator PMC.", paragraphs.get(7).toString());
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

        basePositionsAssert(pages);

        assertEquals(21, pageOneParagraphs.size());
        // Some long line breaking paragraphs should remain as single paragraphs
        assertEquals("Exhibit 10.2", pageOneParagraphs.get(0).toString());
        assertEquals("IN ACCORDANCE WITH ITEM 601(b) OF REGULATION S-K, CERTAIN IDENTIFIED INFORMATION (THE “CONFIDENTIAL INFORMATION”) HAS BEEN EXCLUDED FROM THIS EXHIBIT BECAUSE IT IS BOTH (I) NOT MATERIAL AND (II) WOULD LIKELY CAUSE COMPETITIVE HARM IF PUBLICLY DISCLOSED. THE CONFIDENTIAL INFORMATION IS DENOTED HEREIN BY [*****].", pageOneParagraphs.get(1).toString());
        assertEquals("ZEBRA® PARTNERCONNECT PROGRAM", pageOneParagraphs.get(2).toString());
        assertEquals("ADDENDUM TO", pageOneParagraphs.get(3).toString());
        assertEquals("ZEBRA® PARTNERCONNECT DISTRIBUTOR AGREEMENT", pageOneParagraphs.get(4).toString());
        assertEquals("THIS ADDENDUM (“Addendum”) is made on the 4th day of February 2019 (“Effective Date”) between the following parties: Zebra Technologies International, LLC, with an office at 3 Overlook Point, Lincolnshire IL 60069 (“Zebra”);", pageOneParagraphs.get(5).toString());
        // '-' could be lower in height so make sure we don't split on that
        assertEquals("Zebra Technologies do Brasil - Comércio de Produtos de Informåtica Ltda., a company incorporated and organized under the laws of Brazil, with offices at Av. Magalhäes de Castro, 4800, sala 72-A, Cidade Jardim, CEP 05676-120, Säo Paulo, sp (\"Zebra Brazil\")", pageOneParagraphs.get(6).toString());
        assertEquals("Xplore Technologies Corporation of America, a company with its principal place of business at 8601 RR 2222, Building 2, Suite #100, Austin, Texas 78730, U.S.A. (“Xplore”);", pageOneParagraphs.get(7).toString());
        assertEquals("(collectively \"Zebra\")", pageOneParagraphs.get(8).toString());
        assertEquals("AND", pageOneParagraphs.get(9).toString());
        assertEquals("ScanSource, Inc., a company incorporated in South Carolina, with its registered office at 6 Logue Court, Greenville, South Carolina 29615 (\"ScanSource\").", pageOneParagraphs.get(10).toString());
        assertEquals("ScanSource Latin America, Inc. a ScanSource Affiliate incorporated in Florida, whose registered business address is 1935 NW 87 Avenue, Miami, Florida 33172 (\"ScanSource Latin America\")", pageOneParagraphs.get(11).toString());
        assertEquals("ScanSource Brazil Distribuidora de Technologias, Ltda., a ScanSource Affiliate incorporated and organized under the laws of Brazil, with offices in the City of Säo José dos Pinhais, State of Paranå, at Avenida Rui Barbosa, 2529, Modulos 11 and 12, Bairro Jardim Ipé, CEP: 83055-320, enrolled with the Taxpayer Register (CNPJ/MF) under No. 05.607.657/0001-35 (\"ScanSource Brazil\")", pageOneParagraphs.get(12).toString());
        assertEquals("SCANSOURCE DE MEXICO S. DE R.L. DE C.V., a ScanSource Affiliate incorporated in Mexico, whose registered business address is Calle 4 No. 298, Colonia Franccionamiento Industrial Alce Blanco, Naucalpan de Juarez, Estado de México 53370 (\"ScanSource Mexico\")", pageOneParagraphs.get(13).toString());
        assertEquals("(Collectively \"Distributor')", pageOneParagraphs.get(14).toString());
        assertEquals("\"Zebra\" and the \"Distributor\" are referred to collectively as 'Parties\" and individually as a \"Party\".", pageOneParagraphs.get(15).toString());
        assertEquals("WHEREAS: (A) On February 12, 2014 the Parties entered into an agreement that was renamed, as of April 11, 2016, to: PartnerConnectTM EVM Distribution Agreement, (as amended) (\"Distribution Agreement\"), which relates to Zebra Enterprise Visibility and Mobility ('EVM\") products and services, and which, as acknowledged by the Parties by entering into this Amendment, is in full force and effect and valid as when this Amendment is executed;", pageOneParagraphs.get(16).toString());
        assertEquals("(B) Distributor purchases Products from Zebra under the Distributor Agreement;", pageOneParagraphs.get(17).toString());
        assertEquals("(C)\u200B Zebra has recently completed the acquisition of Xplore, which transaction closed on August 14, 2018;", pageOneParagraphs.get(18).toString());
        assertEquals("(D) Zebra has expanded its products portfolio by adding the product families listed in Exhibit A, that as of the Effective Date hereof are branded Xplore or Motion Computing, thereto (“Xplore Products”); (E) Xplore, now a Zebra Affiliate, is the seller of Xplore Products;", pageOneParagraphs.get(19).toString());
        assertEquals("Source: SCANSOURCE, INC., 10-Q, 5/9/2019", pageOneParagraphs.get(20).toString());

        assertEquals(13, pages.get(1).getParagraphs().size());
        assertEquals("(F) Xplore wishes to sell Xplore Products to Distributor and Distributor wishes to purchase such products from Xplore pursuant to the terms and conditions of the Distributor Agreement by entering into this Addendum; and", pages.get(1).getParagraphs().get(0).toString());

        assertEquals(9, pages.get(2).getParagraphs().size());
        assertEquals("IN WITNESS HEREOF, the Parties have executed this Addendum on the dates specified herein.", pages.get(2).getParagraphs().get(0).toString());

        assertEquals(20, pages.get(3).getParagraphs().size());

        List<PdfParagraph> lastPageParagraphs = pages.get(4).getParagraphs();
        assertEquals(7, lastPageParagraphs.size());
        assertEquals("4. Stock on Hand. Distributor shall use commercially reasonable efforts to maintain thirty (30) days of stock in Distributor’s inventory to support sales. Xplore acknowledges that from time to time, Distributor’s inventory levels may fall below the thirty (30) days goal that is agreed upon by both Parties. If inventory levels fall below the thirty (30) day goal for more than sixty (60) consecutive days, Xplore, upon written notice to Distributor, shall replenish the stock to an amount agreed by both Parties.", lastPageParagraphs.get(0).toString());
        assertEquals("5. Product Return and Stock Rotation. The terms of Section 3 of Schedule 2 of the Distribution Agreement will apply to Xplore Products, provided however that stock rotation allowance for Xplore Products will be based on the net dollar value of Distributor’s purchases in each calendar quarter of Xplore Products and such allowance will be calculated separate and apart from all other Products purchased by Distributor during such period.", lastPageParagraphs.get(1).toString());
        assertEquals("ARTICLE II. DELIVERY OF PRODUCTS", lastPageParagraphs.get(2).toString());
        assertEquals("1. Shipping Terms. Notwithstanding anything to the contrary contained in the Distribution Agreement, and unless notified by Xplore otherwise, shipping terms for Xplore Products will be Delivery Duty Paid (DDP) INCOTERMS® 2010, whereby Distributor’s price, includes all costs of delivery, insurance, import and / or export duties and tariffs. Such prices are exclusive of all federal, state, municipal or other government excise, sales, use, occupational or like taxes in force, and any such taxes shall be assumed and paid for by Distributor in addition to its payment for the Xplore Products. Title and risk of loss to Xplore Products shall pass to Distributor upon delivery to Distributor, as indicated in the Proof of Delivery (PoD) documents. [*****]", lastPageParagraphs.get(3).toString());
        assertEquals("1. At Distributor’s request, Xplore may deliver Xplore Products directly to Program Members or their respective End Users on behalf of Distributor, and in such instances title and risk of loss will pass to Distributor upon delivery to the applicable recipients, as indicated on the PoD documents. Some exclusions may apply, including countries not served by Xplore shipping and importing methods, and/or countries where Xplore Products, are not certified for resale and/or use.", lastPageParagraphs.get(4).toString());
        assertEquals("1. Proof of Delivery (“POD”). Xplore shall provide to Distributor, at no charge, a means for confirming proof of delivery for Xplore Product shipments when requested by Distributor. Xplore shall provide packing slips for all shipments.", lastPageParagraphs.get(5).toString());
        assertEquals("Source: SCANSOURCE, INC., 10-Q, 5/9/2019", lastPageParagraphs.get(6).toString());
    }

    @Test
    public void testPdfParsingWithParagraphPositionsLargeLineHeightDoc() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("msa_indemnification.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        assertEquals(28, pages.size());
        PdfPage pageOne = pages.get(0);
        assertEquals(1, pageOne.getPageNumber());
        List<PdfParagraph> pageOneParagraphs = pageOne.getParagraphs();

        basePositionsAssert(pages);

        assertEquals(6, pageOneParagraphs.size());
        assertEquals("INDEMNIFICATION AGREEMENT", pageOneParagraphs.get(0).toString());
        assertEquals("This Master Service Agreement (the â€œMSAâ€�) is entered into between COMPANY A, LLC (â€œCOMPANY Aâ€�), a Georgia limited liability corporation, and PDX COMPANY INC______________________ (â€œClientâ€� or â€œyouâ€�).", pageOneParagraphs.get(1).toString());
        assertEquals("THIS AGREEMENT is dated for reference this 21st day of October, 2003.", pageOneParagraphs.get(2).toString());
        assertEquals("ï»¿Unless otherwise agreed by the parties in writing, Products and Services acquired by XX under this EMA are solely for XX's and its Aﬃliates\\\" own internal use and not for resale or sub-licensing, e. XX may not assign, delegate or otherwise transfer all or any part of this EMA without prior consent from <COMPANY>.", pageOneParagraphs.get(3).toString());
        assertEquals("If Licensor advises Licensee to remove its facilities, and Licensee refuses to do so, Licensor may remove the facilities and charge the cost and expense of removal to Licensee or deduct the costs and expenses from monies due Licensee under this Agreement, individual Site Licenses or any other agreements. Licensor, in its sole discretion, may allow some or all of Licensee&rsquo;s equipment to remain on Licensor&rsquo;s property. If no such monies are owed, Licensor may invoke any remedies provided herein or at law or equity to recover all monies owed. Except as otherwise provided herein, the fee for use of a Site terminated before the end of the term for that Site License shall not terminate until the later of (1) the eﬀective date of the early termination or (2) the date on which Licensee has removed its equipment and restored the Site in accordance with Section 12(a) or (3) the date on which Licensor notiﬁes Licensee of its election to exercise its option to accept transfer of Licensee's facilities.", pageOneParagraphs.get(4).toString());
        assertEquals("14.26 Subject to Sections 11 (Warranties; Disclaimers) and 12 (Limitation of Damages) above, Cirracore shall indemnify, defend and hold Client and its employees, agents, shareholders, oﬃcers, directors, successors, End Users and assigns harmless from and against any and all claims, damages, liabilities, costs, settlements, penalties and expenses (including attorneysâ€™ fees, expertâ€™s fees and settlement costs) arising out of any.", pageOneParagraphs.get(5).toString());

        List<PdfParagraph> pageFourParagraphs = pages.get(3).getParagraphs();
        assertEquals(3, pageFourParagraphs.size());
        assertEquals("26.5 Distributor shall have the right of ﬁrst refusal to match the delivered cost to JJC Stores for products that are not delivered by Distributor, which shall remain ﬁrm for the duration of that speciﬁc contract. Any changes to the above pricing formula must have the prior written approval of the JJC's Director of Supply Chain Management.", pageFourParagraphs.get(0).toString());
        assertEquals("8.5 To the extent that you provide conﬁdential information to us, we shall protect the secrecy of the conﬁdential information with the same degree of care as we use to protect our own conﬁdential information, but in no event with less than due care.", pageFourParagraphs.get(1).toString());
        assertEquals("(a) Licensee shall at all times and in all respects comply with all federal, state and local laws, ordinance and regulations, including, but not limited to, the Federal Water Pollution Control Act (33 U.S.C. section 1251, et seq.), Resource Conservation and Recovery Act (42 U.S.C. section 6901, et seq.), Safe Drinking Water Act (42 U.S.C. section 300f, et seq.), Toxic Substances Control Act (15 U.S.C. section 2601, et seq.), Clean Air Act (42 U.S.C. section 7401, et seq.), Comprehensive Environmental Response, Compensation and Liability Act (42 U.S.C. section 9601,et seq.), Safe Drinking Water and Toxic Enforcement Act (California Health and Safety Code section 25249.5, et seq.), other applicable provisions of the California Health and Safety Code (section 25100, et seq., and section 39000, et seq.), California Water Code (section 13000, et seq.), and other comparable state laws, regulations and local ordinances relating to industrial hygiene, environmental protection or the use, analysis, generation, manufacture, storage, disposal or transportation of any oil, ﬂammable explosives, asbestos, urea formaldehyde, radioactive materials or waste, or other hazardous, toxic, contaminated or polluting materials, substances or wastes, including, without limitation, any &ldquo;hazardous substances&rdquo; under any such laws, ordinances or regulations (collectively &ldquo;Hazardous Materials Laws&rdquo;). As used in the provisions of this agreement, &ldquo;hazardous materials&rdquo; include any &ldquo;hazardous substance&rdquo; as that term is deﬁned in section 25316 of the California Health and Safety Code or posing a hazard to health or the environment. Except as otherwise expressly permitted in this Agreement, Licensee shall not use, create, store or allow any hazardous materials on the site. Fuel stored in a motor vehicle for the exclusive use in such vehicle is excepted. Back-up generators and the storage of fuel for such generators shall only be allowed if provided in a particular Site License under the conditions of that", pageFourParagraphs.get(2).toString());
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

        basePositionsAssert(pages);

        assertEquals(8, pageOneParagraphs.size());
        assertEquals("INDEMNIFICATION AGREEMENT", pageOneParagraphs.get(0).toString());
        assertEquals("This Master Service Agreement (the â€œMSAâ€) is entered into between COMPANY A, LLC (â€œCOMPANY Aâ€), a Georgia limited liability corporation, and PDX COMPANY INC______________________ (â€œClientâ€ or â€œyouâ€).", pageOneParagraphs.get(1).toString());
        assertEquals("THIS AGREEMENT is dated for reference this 21st day of October, 2003.", pageOneParagraphs.get(2).toString());
        assertEquals("ï»¿Unless otherwise agreed by the parties in writing, Products and Services acquired by XX under this EMA are solely for XX's and its Affiliates\\\" own internal use and not for resale or sub-licensing, e. XX may not assign, delegate or otherwise transfer all or any part of this EMA without prior consent from <COMPANY>.", pageOneParagraphs.get(3).toString());
        assertEquals("If Licensor advises Licensee to remove its facilities, and Licensee refuses to do so, Licensor may remove the facilities and charge the cost and expense of removal to Licensee or deduct the costs and expenses from monies due Licensee under this Agreement, individual Site Licenses or any other agreements. Licensor, in its sole discretion, may allow some or all of Licensee&rsquo;s equipment to remain on Licensor&rsquo;s property. If no such monies are owed, Licensor may invoke any remedies provided herein or at law or equity to recover all monies owed. Except as otherwise provided herein, the fee for use of a Site terminated before the end of the term for that Site License shall not terminate until the later of (1) the effective date of the early termination or (2) the date on which Licensee has removed its equipment and restored the Site in accordance with Section 12(a) or (3) the date on which Licensor notifies Licensee of its election to exercise its option to accept transfer of Licensee's facilities.", pageOneParagraphs.get(4).toString());
        assertEquals("14.26 Subject to Sections 11 (Warranties; Disclaimers) and 12 (Limitation of Damages) above, Cirracore shall indemnify, defend and hold Client and its employees, agents, shareholders, officers, directors, successors, End Users and assigns harmless from and against any and all claims, damages, liabilities, costs, settlements, penalties and expenses (including attorneysâ€™ fees, expertâ€™s fees and settlement costs) arising out of any.", pageOneParagraphs.get(5).toString());
        assertEquals("22.8 B. All Orders shall be paid within 30 days of the date of invoice. If payment is not received by ABAXIS within said 30 days, the payment shall bear a late payment charge equal to 1.5% per month (or partial month) that the payment is delayed.", pageOneParagraphs.get(6).toString());
        assertEquals("23.6 Term: The term of this Agreement shall commence on the date first written above and shall continue until March 31, 2006, provided that Publisher shall have the option, via notice to Atari no later than February 15, 2006, to extend the term for an additional one-year period, through March 31, 2007, on the same terms and conditions (the \"TERM\"). The Term may be extended for one or more additional one (1) year periods via a mutually executed amendment to this Agreement. The three (3) month sell-off period shall commence as of the earlier expiration of this Agreement or upon notice of termination. Upon", pageOneParagraphs.get(7).toString());

        List<PdfParagraph> pageTwoParagraphs = pages.get(1).getParagraphs();
        assertEquals(6, pageTwoParagraphs.size());
        assertEquals("14.58 By DIRECTV. DIRECTV shall indemnify and hold harmless each of Programmer, its Affiliated Companies, Programmer's contractors, subcontractors and authorized distributors, each supplier to Programmer of any portion of the Services hereunder and each participant therein and the directors, officers, employees and agents of Programmer, such Affiliated Companies, such contractors, subcontractors and distributors and such suppliers and participants therein (collectively, the \"Programmer Indemnitees\") from, against and with respect to any and all claims, damages, liabilities, costs and expenses (including reasonable attorneys' and experts' fees) incurred in connection with any third party claim (including, without limitation, a claim by any Governmental Authority) against the Programmer Indemnitees arising out of (i) DIRECTV's breach or alleged breach of any provision of this Agreement, (ii) the distribution by DIRECTV of the Services (except with respect to claims relating to the content of the Services for which Programmer is solely responsible pursuant to Section 8.1(ii) and Section 8.1(iii)), (iii) DIRECTV's advertising and marketing of the Services (except with respect to such advertising and marketing materials or content supplied or approved by Programmer), and (iv) any other materials, including advertising or promotional copy, supplied by DIRECTV. In addition, DIRECTV shall pay and hold Programmer harmless from any federal, state, or local taxes or fees, including any fees payable to local franchising authorities, which are based upon revenues derived by, or the operations of, DIRECTV.", pageTwoParagraphs.get(0).toString());
        assertEquals("25. RightToAssign", pageTwoParagraphs.get(1).toString());
        assertEquals("28.12 Termination by Emergent without Cause. Notwithstanding anything contained herein to the contrary, Emergent shall have the right to terminate this Agreement in its entirety or with respect to one or more countries in the Territory at any time in its sole discretion by giving one hundred and eighty (180) daysâ€™ written notice to Supplier.", pageTwoParagraphs.get(2).toString());
        assertEquals("14.36 (c) The relevant Borrower shall indemnify the Administrative Agent and each Lender, within 10 days after written demand therefor, for the full amount of any Indemnified Taxes or Other Taxes paid by the Administrative Agent or such Lender, as the case may be, on or with respect to any payment by or on account of any obligation of any Borrower hereunder (including Indemnified Taxes or Other Taxes imposed or asserted on or attributable to amounts payable under this Section), and any penalties, interest and reasonable expenses arising therefrom or with respect thereto, whether or not such Indemnified Taxes or Other Taxes were correctly or legally imposed or asserted by the relevant Governmental Authority. A certificate as to the amount of such payment or liability delivered to the Company by a Lender, or by the Administrative Agent on its own behalf or on behalf of a Lender, shall be conclusive absent manifest error.", pageTwoParagraphs.get(3).toString());
        assertEquals("14.10 Except to the extent caused by the negligence of willful misconduct of Tenant Parties, Landlord shall indemnify and hold Tenant harmless from and against any and all claims or liability for any injury or damage to any person or property including any reasonable attorney's fees (but excluding any consequential damages or loss of business) occurring in, on, or about the Project to the extent such injury or damage is caused by the negligence or willful misconduct of Landlord, its employees, its property manager, or its property manager's employees; provided, however, that the foregoing indemnity shall not include claims or liability to the extent waived by Tenant pursuant to Paragraph 10(b) below. Further, (1) in the event of a discrepancy between the terms of this Paragraph and the terms of Paragraph 39 of this Lease concerning Hazardous Substances liability, the latter shall control; and (2) nothing in this Paragraph 10(a) is intended to nor shall it be deemed to override the provisions of Paragraph 11.", pageTwoParagraphs.get(4).toString());
        assertEquals("9. DutyToReturn", pageTwoParagraphs.get(5).toString());

        assertEquals(6, pages.get(2).getParagraphs().size());

        List<PdfParagraph> pageFourParagraphs = pages.get(3).getParagraphs();
        assertEquals(7, pageFourParagraphs.size());
        assertEquals("25.1 Assignments. This Agreement shall be freely assignable by Company to and shall inure to the benefit of, and be binding upon, Company, its successors and assigns and/or any other entity which shall succeed to the business presently being conducted by Company. Being a contract for personal services, neither this Agreement nor any rights hereunder shall be assigned by Employee.", pageFourParagraphs.get(0).toString());
        assertEquals("5.28 Neither this Agreement nor any of the parties' rights hereunder shall be assignable by any party hereto without the prior written consent of the other party hereto.", pageFourParagraphs.get(1).toString());
        assertEquals("WHEREAS Licensor may seek to provide the traveling public with wireless telephone access to traffic information lines. If Licensor does so, Licensee shall cooperate in developing a program to provide the traveling public with wireless telephone access to information lines, and to create an emergency access line subject to Licensee's operational capacity; NOW THEREFORE, in consideration of the mutual covenants and benefits stated herein, and in further consideration of the obligations, terms and considerations hereinafter set forth and recited; Licensor and Licensee agree as follows:", pageFourParagraphs.get(2).toString());
        assertEquals("15.2 CONSEQUENTIAL DAMAGES WAIVER. IN NO EVENT SHALL EITHER PARTY BE LIABLE TO THE OTHER FOR ANY INCIDENTAL OR CONSEQUENTIAL LOSSES OR DAMAGES (INCLUDING, BUT NOT LIMITED TO ECONOMIC LOSS OR LOSS OF PROFITS BY HEINZ) SUFFERED OR INCURRED AS A RESULT OF OR IN CONNECTION WITH ANY BREACH OF THIS AGREEMENT OR ANY TORT (INCLUDING, BUT NOT LIMITED TO, STRICT LIABILITY OR NEGLIGENCE) COMMITTED BY A PARTY IN CONNECTION WITH THIS AGREEMENT.", pageFourParagraphs.get(3).toString());
        assertEquals("15.3 NO LIABILITY FOR CERTAIN DAMAGES. TO THE MAXIMUM EXTENT PERMITTED BY APPLICABLE LAW, NEITHER PARTY NOR THEIR AFFILIATES, SUPPLIERS OR CONTRACTORS WILL BE LIABLE FOR ANY INDIRECT DAMAGES (INCLUDING WITHOUT LIMITATION, CONSEQUENTIAL, SPECIAL, OR INCIDENTAL DAMAGES, DAMAGES FOR LOSS OF PROFITS OR REVENUES, BUSINESS INTERRUPTION, OR LOSS OF BUSINESS INFORMATION), ARISING IN CONNECTION WITH THIS AGREEMENT, ANY STATEMENT OF SERVICES, SERVICES, SERVICE DELIVERABLES, FIXES, PRODUCTS, OR ANY OTHER MATERIALS OR INFORMATION, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGES OR IF SUCH POSSIBILITY WAS REASONABLY FORESEEABLE. THIS EXCLUSION OF LIABILITY DOES NOT APPLY TO EITHER PARTY'S LIABILITY TO THE OTHER FOR VIOLATION OF ITS CONFIDENTIALITY OBLIGATION, REDISTRIBUTION OR OF THE OTHER PARTY'S INTELLECTUAL PROPERTY RIGHTS.", pageFourParagraphs.get(4).toString());
        assertEquals("4.3 The Option Agreement will provide that the subject options will vest in the event of a Change in Control or a Public Offering (as such terms are defined in the Option Agreement).", pageFourParagraphs.get(5).toString());
        assertEquals("23.5 Term. The Term of this Agreement shall extend for three years from the Effective Date, unless terminated earlier as permitted in this Section 5 below. The Term may be renewed as mutually agreed to by the parties.", pageFourParagraphs.get(6).toString());

        assertEquals(6, pages.get(4).getParagraphs().size());
        assertEquals(6, pages.get(5).getParagraphs().size());
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

        basePositionsAssert(pages);

        assertEquals(13, pageOneParagraphs.size());
        assertEquals("EXHIBIT 10.18", pageOneParagraphs.get(0).toString());
        assertEquals("SOFTWARE LICENSE AGREEMENT", pageOneParagraphs.get(1).toString());
        assertEquals("THIS SOFTWARE LICENSE AGREEMENT (the “\u200BAgreement\u200B”) is made as of December 9, 2014 (the “\u200BEffective Date\u200B”) by and between Pear Therapeutics, Inc. (“\u200BPear\u200B”), a Delaware corporation having its principal place of business at 55 Temple Place, Floor 3, Boston MA 02111, and Behavioural Neurological Applications and Solutions Inc. (“\u200BLicensor\u200B”), having his principal place of business at 100 College Street, Suite 213, Toronto, ON M5G 1L5.", pageOneParagraphs.get(2).toString());
        assertEquals("WHEREAS Licensor owns and operates a suite of software applications called Megateam, which are software solutions for treating mental health conditions including ADHD (i.e., the Applications);", pageOneParagraphs.get(3).toString());
        assertEquals("2.1.1 \u200BCreate Combination Products\u200B: to combine and package the Licensor Products with pharmaceutical", pageOneParagraphs.get(12).toString());

        assertEquals(10, pages.get(1).getParagraphs().size());

        List<PdfParagraph> pageThreeParagraphs = pages.get(2).getParagraphs();
        assertEquals(11, pageThreeParagraphs.size());
        assertEquals("Products in connection with the creation of Combination Products), and non-exclusive with respect to all other rights granted to Pear in the Licensor Products under Section 2.1. In addition, all of the foregoing rights granted under Section 2.1 will be permissive but not obligatory; meaning that (unless otherwise expressly set forth in the Agreement to the contrary) Pear will not be under any obligation to use the Licensor Products and/or the Content in any manner whatsoever unless it so chooses.", pageThreeParagraphs.get(0).toString());
        assertEquals("2.2 \u200BRestrictions\u200B. Except as expressly permitted in this Agreement, Pear will not (and will not permit third parties to):", pageThreeParagraphs.get(1).toString());
        assertEquals("(a) (except as necessary to exercise the rights set forth in Section 2.1, above) distribute, rent, or otherwise transfer any rights in the Licensor Products;", pageThreeParagraphs.get(2).toString());
        assertEquals("(b) except as necessary in the creation of Integrated Products, or pursuant to Section 3.17, modify the Applications. In addition, changes to the Integrated Products that involve further modifications of any customized version of the Applications provided by Licensor will be subject to mutual agreement of the parties, including around the reasonable costs involved to make any such changes;", pageThreeParagraphs.get(3).toString());
        assertEquals("(c) except as permitted by applicable law, reverse engineer, decrypt, decompile, or disassemble the Licensor Products; or", pageThreeParagraphs.get(4).toString());
        assertEquals("(d) use the Licensor Products in any manner or for any purpose not authorized or contemplated by this", pageThreeParagraphs.get(5).toString());
        assertEquals("Agreement.", pageThreeParagraphs.get(6).toString());
        assertEquals("2.3 \u200BLimitations on License\u200B. Notwithstanding the foregoing, if Pear does not secure the rights to sell Acceptable Drugs in conjunction with the Combination Products, the license granted pursuant to Section 2.1.2(a) will revert to a non-exclusive license.", pageThreeParagraphs.get(7).toString());
        assertEquals("2", pageThreeParagraphs.get(8).toString());
        assertEquals("3. RESPONSIBILITIES OF THE PARTIES.", pageThreeParagraphs.get(9).toString());
        assertEquals("3.1 \u200BResponsibilities of Licensor\u200B. Licensor will have primary responsibility for performing the following tasks and providing the following services:", pageThreeParagraphs.get(10).toString());

        assertEquals(8, pages.get(3).getParagraphs().size());
        assertEquals(8, pages.get(4).getParagraphs().size());
        assertEquals(8, pages.get(5).getParagraphs().size());
        assertEquals(6, pages.get(6).getParagraphs().size());
    }

    @Test
    public void testPdfParsingWithVaryingLineHeightLargePdf() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("CHA_Verizon.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        basePositionsAssert(pages);

        List<PdfParagraph> pageOneParagraphs = pages.get(0).getParagraphs();
        assertEquals(20, pageOneParagraphs.size());
        assertEquals("Exhibit 10.5", pageOneParagraphs.get(0).toString());
        assertEquals("Contract No. 750-67761-2004", pageOneParagraphs.get(1).toString());
        assertEquals("WAP 2.0 HOSTING AGREEMENT", pageOneParagraphs.get(2).toString());
        assertEquals("PREMIUM WIRELESS SERVICES USA, INC.", pageOneParagraphs.get(3).toString());
        assertEquals("D/B/A INFOSPACE MOBILE", pageOneParagraphs.get(4).toString());
        assertEquals("CELLCO PARTNERSHIP", pageOneParagraphs.get(6).toString());
        assertEquals("d/b/a", pageOneParagraphs.get(7).toString());
        assertEquals("VERIZON WIRELESS", pageOneParagraphs.get(8).toString());
        assertEquals("June 24, 2004", pageOneParagraphs.get(9).toString());
        assertEquals("WAP 2.0 HOSTING AGREEMENT", pageOneParagraphs.get(10).toString());
        assertEquals("This WAP 2.0 Hosting Agreement (“Agreement”), dated as of June 24, 2004 (the “Effective Date”), is made by and between Premium Wireless Services USA, Inc., a California corporation and a wholly owned subsidiary of InfoSpace, Inc. (“InfoSpace”), with offices at 10940 Wilshire Blvd., 9\u200Bth\u200B Floor, Los Angeles, CA 90024, and Cellco Partnership d/b/a Verizon Wireless (“Verizon Wireless”), a Delaware general partnership, having an office and principal place of business at 180 Washington Valley Road, Bedminster, New Jersey 07921. InfoSpace and Verizon Wireless are sometimes individually referred to herein as a “Party” and may be collectively referred to as the “Parties.”", pageOneParagraphs.get(11).toString());
        assertEquals("RECITALS", pageOneParagraphs.get(12).toString());
        assertEquals("A. InfoSpace is in the business of, among other things, providing wireless infrastructure products and services to its customers.", pageOneParagraphs.get(13).toString());
        assertEquals("B. Verizon Wireless is in the business of, among other things, providing wireless telecommunications services to its customers.", pageOneParagraphs.get(14).toString());
        assertEquals("C. Verizon Wireless desires that InfoSpace provide access to the products and services more particularly described on Exhibit B (collectively, the “Portal Services”), and InfoSpace is willing to provide access to the Portal Services to Verizon Wireless, pursuant to and in accordance with the terms and conditions set forth in this Agreement.", pageOneParagraphs.get(15).toString());
        assertEquals("AGREEMENT", pageOneParagraphs.get(16).toString());
        assertEquals("Now, therefore, in consideration of the foregoing, and for other good and valuable consideration, the receipt and sufficiency of which are hereby acknowledged, the Parties agree as follows:", pageOneParagraphs.get(17).toString());
        assertEquals("1 Definitions and Exhibits", pageOneParagraphs.get(18).toString());
        assertEquals("All capitalized terms shall have the meanings ascribed to them in Exhibit A or as otherwise defined in this Agreement. All exhibits attached to this Agreement are hereby incorporated into, and are an integral part of this", pageOneParagraphs.get(19).toString());

        List<PdfParagraph> pageTwoParagraphs = pages.get(1).getParagraphs();
        assertEquals(13, pageTwoParagraphs.size());
        assertEquals("Agreement.", pageTwoParagraphs.get(0).toString());
        assertEquals("2 Rights and Obligations of the Parties 2.1\u200B \u200BInfoSpace Services.\u200B Subject to the terms and conditions of this Agreement and during the Term, InfoSpace will make available to Verizon Wireless the Portal Services described in Exhibit B.", pageTwoParagraphs.get(1).toString());
        assertEquals("2.2 Verizon Wireless Materials.\u200B Subject to the terms and conditions of this Agreement and during the Term, Verizon Wireless hereby grants to InfoSpace the right to include and implement the Verizon Wireless Materials on the Portal Services.", pageTwoParagraphs.get(2).toString());
        assertEquals("2.3\u200B \u200BAccess to Adult Content.\u200B If Verizon Wireless elects to make available adult content under the terms of this Agreement, the Parties shall mutually agree on the terms and conditions governing such availability.", pageTwoParagraphs.get(3).toString());
        assertEquals("PAGE 1", pageTwoParagraphs.get(4).toString());
        assertEquals("2.4 Limitations.", pageTwoParagraphs.get(5).toString());
        assertEquals("a. Other than as explicitly set forth herein, Verizon Wireless and its Affiliates shall have no right to reproduce or sub-license, re-sell or otherwise distribute all or any portion of the Portal Services to any Person except that Verizon Wireless may distribute the Portal Services through its direct distribution channel including its communication stores, websites and its indirect distribution channel, including its authorized agents, retailers and subagents, provided that such distribution and/or sub-distribution is solely within the United States.", pageTwoParagraphs.get(6).toString());
        assertEquals("b. Unless otherwise agreed to by the Parties, Verizon Wireless shall not authorize or assist any Third Party to: (i) remove, obscure, or alter any legal notices, including notices of Intellectual Property Rights present on or in the Portal Services or any other materials provided by InfoSpace, or (ii) insert any interstitial advertisements, pop-up windows, or other items or techniques that would alter the appearance or presentation of the Portal Services.", pageTwoParagraphs.get(7).toString());
        assertEquals("c. InfoSpace shall not itself, and neither shall it authorize nor assist any Third Party in: (i) removing, obscuring, or altering any legal notices, including notices of Intellectual Property Rights present on or in the Verizon Wireless Materials or any other materials provided by Verizon Wireless, or (ii) insert any interstitial advertisements, pop-up windows, or other items or techniques that would alter the appearance or presentation of the Verizon Wireless Materials or the Verizon Wireless Services.", pageTwoParagraphs.get(8).toString());
        assertEquals("d. Other than in connection with its performance under this Agreement, InfoSpace and its Affiliates shall have no right under this Agreement to reproduce or sub-license, re-sell or otherwise distribute all or any portion of the Verizon Wireless Materials to any Person.", pageTwoParagraphs.get(9).toString());
        assertEquals("e. Each Party shall comply with all then-current applicable laws, rules, and regulations in connection with the exercise of its respective rights and obligations under this Agreement (including, without limitation, any law, rule or regulation related to individual privacy).", pageTwoParagraphs.get(10).toString());
        assertEquals("f. Neither Party will reverse engineer, disassemble, decompile or otherwise attempt to discover the source code or trade secrets for any of the technology belonging to the other Party. 2.5\u200B \u200BTechnical Cooperation. \u200BEach of the Parties agree to provide reasonable technical cooperation to the other Party in order to implement the Portal Services. In addition, Verizon Wireless shall allow InfoSpace to implement, and/or shall cooperate with InfoSpace upon its request to assist with its implementation of any bug fixes or updates to the Portal Services.", pageTwoParagraphs.get(11).toString());
        assertEquals("2.6\u200B \u200BProject Management. \u200BEach Party will appoint a single primary point of contact for project management and coordination. This individual will be responsible for coordinating internal teams and activities associated with the deployment of the Portal Services; prioritizing issues and change requests; providing internal communication of project schedule and status; and coordinating meetings and other joint activities between the Parties.", pageTwoParagraphs.get(12).toString());

        List<PdfParagraph> pageEightParagraphs = pages.get(7).getParagraphs();
        assertEquals(19, pageEightParagraphs.size());
        assertEquals("Service Level Agreement,***.", pageEightParagraphs.get(0).toString());
        assertEquals("5.3 Other Operational Obligations.", pageEightParagraphs.get(1).toString());
        assertEquals("a. Network. During the Term, Verizon Wireless shall use commercially reasonable efforts to maintain the Verizon Wireless Network, and shall provide Users with access to the Portal Services via such Verizon Wireless Network.", pageEightParagraphs.get(2).toString());
        assertEquals("b. Portal Services Security.", pageEightParagraphs.get(3).toString());
        assertEquals("To the extent a component of the Portal Services is owned by or under the control of either of the respective Parties, and in addition to the Parties’ specific obligations with regard to viruses (as stated in Section 5.4 of this Agreement), each Party agrees to use reasonable and good faith efforts to maintain the security and integrity of said components.", pageEightParagraphs.get(4).toString());
        assertEquals("*** This redacted material has been omitted pursuant to a request for confidential treatment, and the material has been filed separately with the Commission.", pageEightParagraphs.get(5).toString());
        assertEquals("PAGE 9", pageEightParagraphs.get(6).toString());
        assertEquals("i. In the event that the security and/or integrity of the Portal Services, or any component thereof, is somehow compromised, the Parties agree to notify the other Party of such security compromise as ***, and to use their best efforts to cure said compromise ***.", pageEightParagraphs.get(7).toString());
        assertEquals("ii. As a part of using their best efforts to cure any compromise, the Parties shall: (A) ***,", pageEightParagraphs.get(8).toString());
        assertEquals("(B) promptly remove from the Portal Services element(s) affected by the compromise,", pageEightParagraphs.get(9).toString());
        assertEquals("(C) promptly remedy any adverse condition caused by the compromise,", pageEightParagraphs.get(10).toString());
        assertEquals("(D) ***,", pageEightParagraphs.get(11).toString());
        assertEquals("(E) ***", pageEightParagraphs.get(12).toString());
        assertEquals("(F) *** reinstate any such Portal Services elements that have been removed ***, consistent with the goal of avoiding any further compromise.", pageEightParagraphs.get(13).toString());
        assertEquals("iii. With respect to compromises for which InfoSpace is solely responsible, if the compromise persists after implementing the remedies set forth in this Section, Verizon Wireless may terminate this Agreement ***.", pageEightParagraphs.get(14).toString());
        assertEquals("iv. To the extent the process for addressing a compromise set forth in this Section conflicts with the Service Level Agreement, ***.", pageEightParagraphs.get(15).toString());
        assertEquals("c. Unsolicited Data or Messaging (“Spam”). The Parties agree to implement procedures and to use commercially reasonable efforts to prevent Third Parties from sending or transmitting unsolicited WAP push or SMS messages to Users. Each Party agrees to notify the other Party if it knows or has reason to know that Spam is being sent to Users by Third Parties, and agrees to use commercially reasonable efforts to prevent and/or block", pageEightParagraphs.get(16).toString());
        assertEquals("*** This redacted material has been omitted pursuant to a request for confidential treatment, and the material has been filed separately with the Commission.", pageEightParagraphs.get(17).toString());
        assertEquals("PAGE 10", pageEightParagraphs.get(18).toString());

        List<PdfParagraph> lastPageParagraphs = pages.get(pages.size() - 1).getParagraphs();
        assertEquals("PAGE 129", lastPageParagraphs.get(lastPageParagraphs.size() - 1).toString());
        assertEquals("*** This redacted material has been omitted pursuant to a request for confidential treatment, and the material has been filed separately with the Commission.", lastPageParagraphs.get(lastPageParagraphs.size() - 2).toString());
    }

    @Test
    public void testPdfWithSingleLineAndWordParagraphsAndSingleLinePage() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("SMITHELECTRICVEHICLESCO_FLEET MAINTENANCE.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        basePositionsAssert(pages);

        List<PdfParagraph> pageOneParagraphs = pages.get(0).getParagraphs();
        assertEquals(21, pageOneParagraphs.size());
        assertEquals("EXHIBIT 10.26", pageOneParagraphs.get(0).toString());
        assertEquals("***Confidential treatment requested pursuant to a request for confidential treatment filed with the Securities and Exchange Commission. Omitted portions have been filed separately with the Commission.", pageOneParagraphs.get(1).toString());
        assertEquals("FLEET MAINTENANCE AGREEMENT", pageOneParagraphs.get(2).toString());
        assertEquals("1. Definitions", pageOneParagraphs.get(3).toString());
        assertEquals("1.1 In this Agreement:", pageOneParagraphs.get(4).toString());
        assertEquals("1.1.1 the following expressions have the following meanings unless inconsistent with the context:", pageOneParagraphs.get(5).toString());
        assertEquals("“the Act” means the Employment Rights Act 1996.", pageOneParagraphs.get(6).toString());
        assertEquals("“Additional Charges” means the charges to be calculated by SEV on a time and materials basis at the rates described in Clause 7 of this Agreement in respect of the provision of Excepted Services pursuant to Clause 6 of this Agreement.", pageOneParagraphs.get(7).toString());
        assertEquals("“Agreement” means this agreement including the Schedules and the appendix made between SEV and DCL", pageOneParagraphs.get(8).toString());
        assertEquals("“Bodywork” means, without limitation, the panels, doors, glazing, trim, seating and any custom built additions not supplied by the original Vehicle manufacturer", pageOneParagraphs.get(9).toString());
        assertEquals("“CDV” means an Engine powered car derived van included in this Agreement", pageOneParagraphs.get(10).toString());
        assertEquals("“Charger” means the battery charger and related equipment of an EGV.", pageOneParagraphs.get(11).toString());
        assertEquals("“Chassis” means the main frame, sub-frames and mounting brackets of the vehicle", pageOneParagraphs.get(12).toString());
        assertEquals("“Code of Practice” means the HMSO code of practice set out in the appendix", pageOneParagraphs.get(13).toString());
        assertEquals("“Commencement Date” means 16 October 2005.", pageOneParagraphs.get(14).toString());
        assertEquals("“Contracted Period” means the period during which this Agreement is in effect.", pageOneParagraphs.get(15).toString());
        assertEquals("“Contract Procedure Manual” means a separate operating manual that identifies procedures and documentation relevant to this Agreement.", pageOneParagraphs.get(16).toString());
        assertEquals("“DCL Financial Year” means the period of 12 (twelve) months commencing on the first day of each financial year of DCL during the term of this Agreement as notified by DCL to SEV in writing or as otherwise agreed between the parties in writing (and, in the", pageOneParagraphs.get(17).toString());

        // These are actually higher in the page, per coordinates, but not according to pdf box
        assertEquals("DATED 13 October 2005", pageOneParagraphs.get(18).toString());
        assertEquals("DCL DAIRY CREST LIMITED (Company no 2085882) whose registered office is at Claygate House, Littleworth Road, Esher, Surrey KT10 9PN", pageOneParagraphs.get(19).toString());
        assertEquals("SEV SEV GROUP LIMITED (company no 4463640) whose registered office is at Unit 95/2, Tanfield Lea Industrial Estate North, Stanley, Co Durham, DH9 9NX", pageOneParagraphs.get(20).toString());

        // Make sure we handle single line, one paragraph only, page
        List<PdfParagraph> lastPageParagraphs = pages.get(pages.size() - 1).getParagraphs();
        assertEquals(1, lastPageParagraphs.size());
        assertEquals("DATE 19-1-06", lastPageParagraphs.get(0).toString());
    }

    @Test
    public void testPdfLinesEndingWithNBSP() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("RoyaleEnergy.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();
        assertEquals(5, pages.size());

        basePositionsAssert(pages);

        List<PdfParagraph> pageOneParagraphs = pages.get(0).getParagraphs();
        assertEquals(12, pageOneParagraphs.size());
        assertEquals("Exhibit 10.10", pageOneParagraphs.get(0).toString());
        assertEquals("CONSULTING AGREEMENT", pageOneParagraphs.get(1).toString());
        assertEquals("This Consulting Agreement (this “\u200BAgreement\u200B\u200B”) is entered into effective as of March 1, 2018, by and between Royale Energy, Inc., (“\u200BRoyale\u200B\u200B”), and Meeteetse Limited Partnership (tax ID 56-2298132) (“\u200BConsultant\u200B\u200B”).", pageOneParagraphs.get(2).toString());

        List<PdfParagraph> pageThreeParagraphs = pages.get(2).getParagraphs();
        assertEquals(6, pageThreeParagraphs.size());
        assertEquals("association, or other entity for any reason or purpose whatsoever, except as is generally available to the public or as specifically allowed in writing by an authorized representative of Royale. This \u200Bsubsection (b) will indefinitely survive the expiration or termination of this Agreement.", pageThreeParagraphs.get(0).toString());
        assertEquals("(c)     \u200B\u200BReturn of Confidential Information\u200B\u200B. Upon the expiration of the term or termination of this Agreement, Consultant will surrender to Royale all tangible Confidential Information in the possession of, or under the control of, Consultant, including, but without limitation, the originals and all copies of all software, drawings, manuals, letters, notes, notebooks, reports, and all other media, material, and records of any kind, and all copies thereof pertaining to Confidential Information acquired or developed by Consultant during the term of Consultant's employment (including the period preceding the Effective Date).", pageThreeParagraphs.get(1).toString());
        assertEquals("(d)     \u200B\u200BNon-Solicitation\u200B\u200B. During the term of this Agreement and for a period of one year after termination of the Agreement (the Applicable Period), Consultant will not induce, or attempt to induce, any employee or independent contractor of Royale to cease such employment or contractual relationship with Royale. Consultant furthermore agrees that in the event an employee or independent contractor terminates their employment or contractual relationship with Royale, or such employee or independent contractor is terminated by Royale, Consultant, without the prior written consent of Royale will not, during the Applicable Period, directly or indirectly, offer employment to, employ, or enter into any agreement or contract with (whether written or oral) such employee or independent contractor, or in any other manner deal with such employee or contractor; \u200Bprovided\u200B, that the provisions of this subsection (d) shall not apply to contacts with or solicitations of any person who was an associated person of Royale immediately prior to execution of the Purchase Agreement.", pageThreeParagraphs.get(2).toString());
        assertEquals("(e)     \u200B\u200BRight to Injunctive Relief\u200B\u200B. Consultant acknowledges that a violation or attempted violation on his part of any agreement in this \u200BSection 5 will cause irreparable damage to the Royale and its affiliates, and accordingly Consultant agrees that the Royale shall be entitled as a manner of right to an injunction, out of any court of competent jurisdiction restraining any violation or further violation of such agreements by Consultant; such right to an injunction, however, shall be cumulative and in addition to whatever other remedies the Royale may have. The terms and agreements set forth in this \u200BSection 5 shall survive the expiration of the term or termination of this Agreement for any reason. The existence of any claim of Consultant, whether predicated on this Agreement or otherwise, shall not constitute a defense to the enforcement by the Royale of the agreements contained in this Section 5\u200B.", pageThreeParagraphs.get(3).toString());
        assertEquals("Page 3", pageThreeParagraphs.get(4).toString());
        assertEquals("(f)     \u200B\u200BIndependent Contractor\u200B\u200B. Royale and Consultant agree that in the performance of the services contemplated herein, Consultant shall be, and is, an independent contractor, and this Agreement shall not be construed to create any association, partnership, joint venture, employee, or agency relationship between Consultant and Royale for any purpose. Consultant will be responsible for tools, equipment and all other supplies needed to fully perform its services under the contract. Consultant has and shall retain the right to exercise full control over the employment, direction, compensation and discharge of all persons assisting Consultant. Consultant shall be solely responsible for, and shall hold Royale harmless from all matters relating to the payment of Consultant’s employees, including compliance with the Social Security Administration, Internal Revenue Service, withholdings and all other regulations governing such matters. Consultant has no authority (and shall not hold itself out as having authority) to bind Royale and Consultant shall not make any agreements or representations on Royale’s behalf without Royale’s prior written consent. Consultant and its employees or contractors will not be eligible to participate in any vacation, group medical or life insurance, disability, profit sharing or retirement benefits, or any other fringe benefits or benefit plans offered by Royale to its employees, and Royale will not be responsible for withholding or paying any income, payroll, Social Security, or other federal, state, or local taxes, making any insurance contributions, including for unemployment or disability, or obtaining workers' compensation insurance on Consultant’s behalf. Consultant shall be responsible for, and shall indemnify Royale against, all such taxes or contributions, including penalties and ", pageThreeParagraphs.get(5).toString());
    }

    @Test
    public void testPdfWithMultiColumnPages() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("reaserach-paper.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();
        assertEquals(14, pages.size());

        basePositionsAssert(pages);

        List<PdfParagraph> pageOneParagraphs = pages.get(0).getParagraphs();
        assertEquals(9, pageOneParagraphs.size());
        assertEquals("Large-scale Incremental Processing", pageOneParagraphs.get(0).toString());
        assertEquals("Using Distributed Transactions and Notiﬁcations", pageOneParagraphs.get(1).toString());
        assertEquals("Daniel Peng and Frank Dabek dpeng@google.com, fdabek@google.com Google, Inc.", pageOneParagraphs.get(2).toString());
        assertEquals("Abstract", pageOneParagraphs.get(3).toString());
        assertEquals("Updating an index of the web as documents are crawled requires continuously transforming a large repository of existing documents as new documents ar- rive. This task is one example of a class of data pro- cessing tasks that transform a large repository of data via small, independent mutations. These tasks lie in a gap between the capabilities of existing infrastructure. Databases do not meet the storage or throughput require- ments of these tasks: Google’s indexing system stores tens of petabytes of data and processes billions of up- dates per day on thousands of machines. MapReduce and other batch-processing systems cannot process small up- dates individually as they rely on creating large batches for efﬁciency. We have built Percolator, a system for incrementally processing updates to a large data set, and deployed it to create the Google web search index. By replacing a batch-based indexing system with an indexing system based on incremental processing using Percolator, we process the same number of documents per day, while reducing the average age of documents in Google search results by 50%.", pageOneParagraphs.get(4).toString());
        assertEquals("1 Introduction", pageOneParagraphs.get(5).toString());
        assertEquals("Consider the task of building an index of the web that can be used to answer search queries. The indexing sys- tem starts by crawling every page on the web and pro- cessing them while maintaining a set of invariants on the index. For example, if the same content is crawled un- der multiple URLs, only the URL with the highest Page- Rank [28] appears in the index. Each link is also inverted so that the anchor text from each outgoing link is at- tached to the page the link points to. Link inversion must work across duplicates: links to a duplicate of a page should be forwarded to the highest PageRank duplicate if necessary. This is a bulk-processing task that can be expressed as a series of MapReduce [13] operations: one for clus- tering duplicates, one for link inversion, etc. It’s easy to maintain invariants since MapReduce limits the paral-", pageOneParagraphs.get(6).toString());
        assertEquals("lelism of the computation; all documents ﬁnish one pro- cessing step before starting the next. For example, when the indexing system is writing inverted links to the cur- rent highest-PageRank URL, we need not worry about its PageRank concurrently changing; a previous MapRe- duce step has already determined its PageRank. Now, consider how to update that index after recrawl- ing some small portion of the web. It’s not sufﬁcient to run the MapReduces over just the new pages since, for example, there are links between the new pages and the rest of the web. The MapReduces must be run again over the entire repository, that is, over both the new pages and the old pages. Given enough computing resources, MapReduce’s scalability makes this approach feasible, and, in fact, Google’s web search index was produced in this way prior to the work described here. However, reprocessing the entire web discards the work done in earlier runs and makes latency proportional to the size of the repository, rather than the size of an update. The indexing system could store the repository in a DBMS and update individual documents while using transactions to maintain invariants. However, existing DBMSs can’t handle the sheer volume of data: Google’s indexing system stores tens of petabytes across thou- sands of machines [30]. Distributed storage systems like Bigtable [9] can scale to the size of our repository but don’t provide tools to help programmers maintain data invariants in the face of concurrent updates. An ideal data processing system for the task of main- taining the web search index would be optimized for in- cremental processing; that is, it would allow us to main- tain a very large repository of documents and update it efﬁciently as each new document was crawled. Given that the system will be processing many small updates concurrently, an ideal system would also provide mech- anisms for maintaining invariants despite concurrent up- dates and for keeping track of which updates have been processed. The remainder of this paper describes a particular in- cremental processing system: Percolator. Percolator pro- vides the user with random access to a multi-PB reposi- tory. Random access allows us to process documents in-", pageOneParagraphs.get(7).toString());
        assertEquals("1", pageOneParagraphs.get(8).toString());
    }

    @Test
    public void testScannedPdfShouldReturnEmptyPagesOnly() throws Exception {
        ParagraphAwarePositionContentHandler contentHandler = new ParagraphAwarePositionContentHandler(new BodyContentHandler(-1));
        parse("EMPTY - Jos A Bank Amendment.pdf", contentHandler);
        List<PdfPage> pages = contentHandler.getPages();

        assertEquals(3, pages.size());
        pages.forEach(page -> {
            assertEquals(0, page.getParagraphs().size());
        });
    }

    private void basePositionsAssert(List<PdfPage> pages) {
        pages.forEach(page -> page.getParagraphs().forEach(para -> {
            assertFalse(para.getTextPositions().isEmpty());
            assertFalse(para.toString().contains("\n"));
            assertTrue(para.getTextPositions().stream().noneMatch(tp -> tp.getUnicode().isEmpty()));

            Set<Map.Entry<Float, List<TextPosition>>> textPositionsPerLine = para.getTextPositions().stream()
                    .collect(Collectors.groupingBy(
                                    TextPosition::getY,
                                    Collectors.filtering(
                                            // Remove ZWSP any other empty, but valid, chars
                                            tp -> tp.getX() != tp.getEndX(),
                                            Collectors.toList())
                            )
                    )
                    .entrySet();

            assertFalse(textPositionsPerLine.isEmpty());

            for (Map.Entry<Float, List<TextPosition>> linePositionsEntry : textPositionsPerLine) {
                List<TextPosition> linePositions = linePositionsEntry.getValue();
                for (int i = 0; i < linePositions.size() - 1; i++) {
                    TextPosition curr = linePositions.get(i);
                    TextPosition next = linePositions.get(i + 1);
                    assertTrue(next.getX() > curr.getX());
                }
            }
        }));
    }
}
