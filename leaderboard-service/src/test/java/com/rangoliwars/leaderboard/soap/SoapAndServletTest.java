package com.rangoliwars.leaderboard.soap;

import com.rangoliwars.leaderboard.TestReports;
import com.rangoliwars.leaderboard.service.LeaderboardService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class SoapAndServletTest {

    @Autowired
    TestRestTemplate http;

    @Autowired
    LeaderboardService leaderboard;

    private ResponseEntity<String> soap(String body) {
        String envelope = """
                <soapenv:Envelope xmlns:soapenv="http://schemas.xmlsoap.org/soap/envelope/"
                                  xmlns:lb="http://rangoliwars.com/leaderboard">
                  <soapenv:Header/>
                  <soapenv:Body>%s</soapenv:Body>
                </soapenv:Envelope>
                """.formatted(body);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_XML);
        return http.postForEntity("/soap", new HttpEntity<>(envelope, headers), String.class);
    }

    @Test
    void wsdlIsPublished() {
        ResponseEntity<String> wsdl = http.getForEntity("/soap/leaderboard.wsdl", String.class);
        assertEquals(HttpStatus.OK, wsdl.getStatusCode());
        assertTrue(wsdl.getBody().contains("LeaderboardPort"));
        assertTrue(wsdl.getBody().contains("getTopPlayers"));
    }

    @Test
    void soapReturnsPlayerStats() {
        leaderboard.recordMatch(TestReports.threePlayer("soap_win", "soap_lose"));

        ResponseEntity<String> stats = soap(
                "<lb:getPlayerStatsRequest><lb:username>soap_win</lb:username></lb:getPlayerStatsRequest>");
        assertEquals(HttpStatus.OK, stats.getStatusCode());
        assertTrue(stats.getBody().contains("soap_win"), stats.getBody());
        assertTrue(stats.getBody().contains("1025"), stats.getBody());

        ResponseEntity<String> top = soap("<lb:getTopPlayersRequest><lb:limit>5</lb:limit></lb:getTopPlayersRequest>");
        assertEquals(HttpStatus.OK, top.getStatusCode());
        assertTrue(top.getBody().contains("getTopPlayersResponse"), top.getBody());
    }

    @Test
    void csvServletNeedsAdminAndStreamsCsv() {
        leaderboard.recordMatch(TestReports.threePlayer("csv_win", "csv_lose"));
        assertEquals(HttpStatus.UNAUTHORIZED,
                http.getForEntity("/admin/export/leaderboard.csv", String.class).getStatusCode());

        ResponseEntity<String> csv = http.withBasicAuth("admin", "test-pass")
                .getForEntity("/admin/export/leaderboard.csv", String.class);
        assertEquals(HttpStatus.OK, csv.getStatusCode());
        assertTrue(csv.getHeaders().getContentType().toString().startsWith("text/csv"));
        assertTrue(csv.getBody().startsWith("rank,username"));
        assertTrue(csv.getBody().contains("csv_win"));
    }
}
