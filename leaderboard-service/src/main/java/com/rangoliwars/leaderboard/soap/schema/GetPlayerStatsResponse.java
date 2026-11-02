package com.rangoliwars.leaderboard.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlElement;
import jakarta.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "getPlayerStatsResponse")
@XmlAccessorType(XmlAccessType.FIELD)
public class GetPlayerStatsResponse {

    @XmlElement(name = "player")
    private SoapPlayerStats player;

    public SoapPlayerStats getPlayer() {
        return player;
    }

    public void setPlayer(SoapPlayerStats player) {
        this.player = player;
    }
}
