package com.rangoliwars.leaderboard.config;

import com.rangoliwars.leaderboard.admin.LeaderboardCsvServlet;
import com.rangoliwars.leaderboard.service.LeaderboardService;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ServletConfig {

    @Bean
    ServletRegistrationBean<LeaderboardCsvServlet> leaderboardCsvServlet(LeaderboardService leaderboard) {
        return new ServletRegistrationBean<>(new LeaderboardCsvServlet(leaderboard), "/admin/export/leaderboard.csv");
    }
}
