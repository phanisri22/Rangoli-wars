package com.rangoliwars.leaderboard.admin;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rangoliwars.leaderboard.service.LeaderboardService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.client.RestClientException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;

/** Server-rendered admin dashboard (JSP). */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private static final TypeReference<List<Map<String, Object>>> ROWS = new TypeReference<>() { };

    private final LeaderboardService leaderboard;
    private final GameServiceClient games;
    private final ObjectMapper mapper;

    public AdminController(LeaderboardService leaderboard, GameServiceClient games, ObjectMapper mapper) {
        this.leaderboard = leaderboard;
        this.games = games;
        this.mapper = mapper;
    }

    @GetMapping
    public String dashboard(Model model) {
        List<?> rooms;
        try {
            rooms = games.rooms();
        } catch (RestClientException e) {
            log.warn("Could not load rooms from game-service: {}", e.getMessage());
            rooms = List.of();
            model.addAttribute("gameServiceDown", true);
        }
        // JSP EL reads maps but not records, so convert
        model.addAttribute("rooms", mapper.convertValue(rooms, ROWS));
        model.addAttribute("players", mapper.convertValue(leaderboard.top(20), ROWS));
        model.addAttribute("matches", mapper.convertValue(leaderboard.recentMatches(15), ROWS));
        return "admin";
    }

    @PostMapping("/rooms/{code}/end")
    public String endRoom(@PathVariable String code, RedirectAttributes redirect) {
        try {
            games.endRoom(code);
            redirect.addFlashAttribute("notice", "Room " + code + " was ended");
        } catch (RestClientException e) {
            redirect.addFlashAttribute("notice", "Could not end room " + code + ": " + e.getMessage());
        }
        return "redirect:/admin";
    }
}
