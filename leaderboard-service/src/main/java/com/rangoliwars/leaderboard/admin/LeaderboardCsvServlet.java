package com.rangoliwars.leaderboard.admin;

import com.rangoliwars.leaderboard.model.Models.PlayerStats;
import com.rangoliwars.leaderboard.service.LeaderboardService;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.io.PrintWriter;

/** Plain servlet that streams the leaderboard as CSV. Registered in ServletConfig. */
public class LeaderboardCsvServlet extends HttpServlet {

    private final transient LeaderboardService leaderboard;

    public LeaderboardCsvServlet(LeaderboardService leaderboard) {
        this.leaderboard = leaderboard;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"leaderboard.csv\"");
        PrintWriter out = response.getWriter();
        out.println("rank,username,games_played,wins,total_tiles,rating");
        for (PlayerStats p : leaderboard.top(100)) {
            out.printf("%d,%s,%d,%d,%d,%d%n",
                    p.rank(), p.username(), p.gamesPlayed(), p.wins(), p.totalTiles(), p.rating());
        }
    }
}
