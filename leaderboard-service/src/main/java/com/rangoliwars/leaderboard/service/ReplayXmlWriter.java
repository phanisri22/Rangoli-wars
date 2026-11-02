package com.rangoliwars.leaderboard.service;

import com.rangoliwars.leaderboard.model.Models.MatchDetail;
import com.rangoliwars.leaderboard.model.Models.MatchSummary;
import com.rangoliwars.leaderboard.model.Models.MoveEntry;
import com.rangoliwars.leaderboard.model.Models.PlayerResult;
import org.springframework.stereotype.Component;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.StringWriter;

/** Writes a match as an XML replay (see replay.xsd) with the JDK StAX API. */
@Component
public class ReplayXmlWriter {

    private final XMLOutputFactory factory = XMLOutputFactory.newFactory();

    public String write(MatchDetail detail) {
        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter xml = factory.createXMLStreamWriter(out);
            MatchSummary m = detail.match();
            xml.writeStartDocument("UTF-8", "1.0");
            xml.writeStartElement("replay");
            xml.writeAttribute("matchId", String.valueOf(m.id()));
            xml.writeAttribute("room", m.roomCode());
            xml.writeAttribute("boardSize", String.valueOf(m.boardSize()));
            xml.writeAttribute("maxRounds", String.valueOf(m.maxRounds()));
            xml.writeAttribute("finishedAt", m.finishedAt().toString());
            xml.writeAttribute("endedEarly", String.valueOf(m.endedEarly()));

            xml.writeStartElement("players");
            for (PlayerResult p : m.players()) {
                xml.writeEmptyElement("player");
                xml.writeAttribute("color", String.valueOf(p.color()));
                xml.writeAttribute("name", p.displayName());
                if (p.username() != null) {
                    xml.writeAttribute("username", p.username());
                }
                xml.writeAttribute("bot", String.valueOf(p.bot()));
                xml.writeAttribute("score", String.valueOf(p.score()));
                xml.writeAttribute("placement", String.valueOf(p.placement()));
            }
            xml.writeEndElement();

            xml.writeStartElement("moves");
            for (MoveEntry move : detail.moves()) {
                xml.writeEmptyElement("move");
                xml.writeAttribute("turn", String.valueOf(move.turn()));
                xml.writeAttribute("round", String.valueOf(move.round()));
                xml.writeAttribute("color", String.valueOf(move.color()));
                xml.writeAttribute("type", move.type());
                xml.writeAttribute("row", String.valueOf(move.row()));
                xml.writeAttribute("col", String.valueOf(move.col()));
                if (move.row2() != null && move.col2() != null) {
                    xml.writeAttribute("row2", String.valueOf(move.row2()));
                    xml.writeAttribute("col2", String.valueOf(move.col2()));
                }
                xml.writeAttribute("gained", String.valueOf(move.gained()));
                xml.writeAttribute("captured", String.valueOf(move.captured()));
            }
            xml.writeEndElement();

            xml.writeEndElement();
            xml.writeEndDocument();
            xml.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("Could not write replay XML", e);
        }
        return out.toString();
    }
}
