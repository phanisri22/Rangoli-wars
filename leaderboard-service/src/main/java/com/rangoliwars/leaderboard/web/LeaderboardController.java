package com.rangoliwars.leaderboard.web;

import com.rangoliwars.leaderboard.model.Models.MatchDetail;
import com.rangoliwars.leaderboard.model.Models.MatchSummary;
import com.rangoliwars.leaderboard.model.Models.PlayerStats;
import com.rangoliwars.leaderboard.service.LeaderboardService;
import com.rangoliwars.leaderboard.service.ReplayXmlWriter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("/api/leaderboard")
public class LeaderboardController {

    private final LeaderboardService leaderboard;
    private final ReplayXmlWriter replayWriter;

    public LeaderboardController(LeaderboardService leaderboard, ReplayXmlWriter replayWriter) {
        this.leaderboard = leaderboard;
        this.replayWriter = replayWriter;
    }

    @GetMapping("/top")
    public List<PlayerStats> top(@RequestParam(defaultValue = "10") int limit) {
        return leaderboard.top(limit);
    }

    @GetMapping("/players/{username}")
    public PlayerStats player(@PathVariable String username) {
        return leaderboard.player(username)
                .orElseThrow(() -> new NoSuchElementException("No games recorded for " + username));
    }

    @GetMapping("/players/{username}/matches")
    public List<MatchSummary> playerMatches(@PathVariable String username,
                                            @RequestParam(defaultValue = "10") int limit) {
        return leaderboard.playerMatches(username, limit);
    }

    @GetMapping("/matches/recent")
    public List<MatchSummary> recent(@RequestParam(defaultValue = "10") int limit) {
        return leaderboard.recentMatches(limit);
    }

    @GetMapping("/matches/{id}")
    public MatchDetail match(@PathVariable long id) {
        return leaderboard.match(id);
    }

    @GetMapping(value = "/matches/{id}/replay.xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<String> replay(@PathVariable long id) {
        String xml = replayWriter.write(leaderboard.match(id));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rangoli-replay-" + id + ".xml\"")
                .contentType(MediaType.APPLICATION_XML)
                .body(xml);
    }
}
