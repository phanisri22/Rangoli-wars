package com.rangoliwars.leaderboard.soap;

import com.rangoliwars.leaderboard.model.Models.PlayerStats;
import com.rangoliwars.leaderboard.service.LeaderboardService;
import com.rangoliwars.leaderboard.soap.schema.GetPlayerStatsRequest;
import com.rangoliwars.leaderboard.soap.schema.GetPlayerStatsResponse;
import com.rangoliwars.leaderboard.soap.schema.GetTopPlayersRequest;
import com.rangoliwars.leaderboard.soap.schema.GetTopPlayersResponse;
import com.rangoliwars.leaderboard.soap.schema.SoapPlayerStats;
import org.springframework.ws.server.endpoint.annotation.Endpoint;
import org.springframework.ws.server.endpoint.annotation.PayloadRoot;
import org.springframework.ws.server.endpoint.annotation.RequestPayload;
import org.springframework.ws.server.endpoint.annotation.ResponsePayload;

@Endpoint
public class LeaderboardSoapEndpoint {

    private final LeaderboardService leaderboard;

    public LeaderboardSoapEndpoint(LeaderboardService leaderboard) {
        this.leaderboard = leaderboard;
    }

    @PayloadRoot(namespace = SoapConfig.NAMESPACE, localPart = "getTopPlayersRequest")
    @ResponsePayload
    public GetTopPlayersResponse topPlayers(@RequestPayload GetTopPlayersRequest request) {
        GetTopPlayersResponse response = new GetTopPlayersResponse();
        int limit = request.getLimit() <= 0 ? 10 : request.getLimit();
        leaderboard.top(limit).forEach(p -> response.getPlayers().add(toSoap(p)));
        return response;
    }

    @PayloadRoot(namespace = SoapConfig.NAMESPACE, localPart = "getPlayerStatsRequest")
    @ResponsePayload
    public GetPlayerStatsResponse playerStats(@RequestPayload GetPlayerStatsRequest request) {
        GetPlayerStatsResponse response = new GetPlayerStatsResponse();
        if (request.getUsername() != null) {
            leaderboard.player(request.getUsername()).ifPresent(p -> response.setPlayer(toSoap(p)));
        }
        return response;
    }

    private static SoapPlayerStats toSoap(PlayerStats p) {
        return new SoapPlayerStats(p.rank(), p.username(), p.gamesPlayed(), p.wins(), p.totalTiles(), p.rating());
    }
}
