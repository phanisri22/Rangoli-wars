package com.rangoliwars.leaderboard.web;

import com.rangoliwars.leaderboard.model.Models.MatchReport;
import com.rangoliwars.leaderboard.model.Models.MatchSaved;
import com.rangoliwars.leaderboard.service.LeaderboardService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InternalMatchController {

    private final LeaderboardService leaderboard;

    public InternalMatchController(LeaderboardService leaderboard) {
        this.leaderboard = leaderboard;
    }

    @PostMapping("/internal/matches")
    @ResponseStatus(HttpStatus.CREATED)
    public MatchSaved record(@Valid @RequestBody MatchReport report) {
        return new MatchSaved(leaderboard.recordMatch(report));
    }
}
