package com.rangoliwars.leaderboard.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

import java.util.ArrayList;
import java.util.List;

@XmlRootElement(name = "getTopPlayersResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetTopPlayersResponse {

    @XmlElement(name = "player")
    private List<SoapPlayerStats> players = new ArrayList<>();

    public List<SoapPlayerStats> getPlayers() {
        return players;
    }
}
