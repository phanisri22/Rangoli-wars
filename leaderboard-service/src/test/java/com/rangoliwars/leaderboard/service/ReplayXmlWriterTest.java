package com.rangoliwars.leaderboard.service;

import com.rangoliwars.leaderboard.model.Models.MatchDetail;
import com.rangoliwars.leaderboard.model.Models.MatchSummary;
import com.rangoliwars.leaderboard.model.Models.MoveEntry;
import com.rangoliwars.leaderboard.model.Models.PlayerResult;
import org.junit.jupiter.api.Test;
import org.w3c.dom.Document;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ReplayXmlWriterTest {

    private static MatchDetail sample() {
        MatchSummary summary = new MatchSummary(7, "ABCDE", 9, 15, Instant.parse("2026-11-01T10:00:00Z"),
                Instant.parse("2026-11-01T10:05:00Z"), false, 2,
                List.of(new PlayerResult(0, "asha", "asha", false, null, 10, 1),
                        new PlayerResult(1, null, "Tara & <Bot>", true, "HARD", 6, 2)));
        List<MoveEntry> moves = List.of(
                new MoveEntry(1, 1, 0, "PLACE", 4, 4, null, null, 1, 0),
                new MoveEntry(2, 1, 1, "SWAP", 4, 5, 4, 4, 0, 0));
        return new MatchDetail(summary, moves);
    }

    @Test
    void outputMatchesTheReplaySchema() throws Exception {
        String xml = new ReplayXmlWriter().write(sample());
        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(getClass().getResource("/xml/replay.xsd"))
                .newValidator()
                .validate(new StreamSource(new StringReader(xml)));
    }

    @Test
    void escapesNamesAndKeepsSwapTarget() throws Exception {
        String xml = new ReplayXmlWriter().write(sample());
        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder()
                .parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));

        assertEquals("Tara & <Bot>",
                doc.getElementsByTagName("player").item(1).getAttributes().getNamedItem("name").getNodeValue());
        var swap = doc.getElementsByTagName("move").item(1).getAttributes();
        assertEquals("4", swap.getNamedItem("row2").getNodeValue());
        assertEquals(2, doc.getElementsByTagName("move").getLength());
        assertFalse(xml.contains("username=\"null\""));
    }
}
