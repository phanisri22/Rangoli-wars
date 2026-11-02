package com.rangoliwars.leaderboard.soap.schema;

import jakarta.xml.bind.annotation.XmlAccessType;
import jakarta.xml.bind.annotation.XmlAccessorType;
import jakarta.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "playerStats", propOrder = {"rank", "username", "gamesPlayed", "wins", "totalTiles", "rating"})
public class SoapPlayerStats {

    private int rank;
    private String username;
    private int gamesPlayed;
    private int wins;
    private int totalTiles;
    private int rating;

    public SoapPlayerStats() {
    }

    public SoapPlayerStats(int rank, String username, int gamesPlayed, int wins, int totalTiles, int rating) {
        this.rank = rank;
        this.username = username;
        this.gamesPlayed = gamesPlayed;
        this.wins = wins;
        this.totalTiles = totalTiles;
        this.rating = rating;
    }

    public int getRank() {
        return rank;
    }

    public String getUsername() {
        return username;
    }

    public int getGamesPlayed() {
        return gamesPlayed;
    }

    public int getWins() {
        return wins;
    }

    public int getTotalTiles() {
        return totalTiles;
    }

    public int getRating() {
        return rating;
    }
}
