<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <title>Rangoli Wars Admin</title>
    <style>
        :root { --bg: #fdf8f2; --card: #fff; --ink: #2b2230; --muted: #7a6e80; --line: #eadfd3; --accent: #c2185b; }
        * { box-sizing: border-box; }
        body { margin: 0; font-family: system-ui, sans-serif; background: var(--bg); color: var(--ink); }
        header { padding: 20px 32px; background: var(--accent); color: #fff; display: flex; justify-content: space-between; align-items: center; }
        header a { color: #fff; }
        main { padding: 24px 32px; display: grid; gap: 24px; }
        section { background: var(--card); border: 1px solid var(--line); border-radius: 10px; padding: 16px 20px; }
        h2 { margin: 0 0 12px; font-size: 18px; }
        table { width: 100%; border-collapse: collapse; font-size: 14px; }
        th, td { text-align: left; padding: 8px; border-bottom: 1px solid var(--line); }
        th { color: var(--muted); font-weight: 600; }
        .notice { background: #fff3cd; border: 1px solid #f0d98c; padding: 10px 14px; border-radius: 8px; }
        .empty { color: var(--muted); }
        button { background: var(--accent); color: #fff; border: 0; border-radius: 6px; padding: 6px 12px; cursor: pointer; }
        .tag { font-size: 12px; padding: 2px 8px; border-radius: 10px; background: #f3e5f5; }
    </style>
</head>
<body>
<header>
    <strong>Rangoli Wars &middot; Admin</strong>
    <a href="<c:url value='/admin/export/leaderboard.csv'/>">Download leaderboard CSV</a>
</header>
<main>
    <c:if test="${not empty notice}">
        <div class="notice"><c:out value="${notice}"/></div>
    </c:if>
    <c:if test="${gameServiceDown}">
        <div class="notice">game-service is not reachable, so live rooms are not shown.</div>
    </c:if>

    <section>
        <h2>Live rooms (${rooms.size()})</h2>
        <c:choose>
            <c:when test="${empty rooms}"><p class="empty">No rooms right now.</p></c:when>
            <c:otherwise>
                <table>
                    <tr><th>Code</th><th>Phase</th><th>Host</th><th>Players</th><th>Board</th><th>Round</th><th></th></tr>
                    <c:forEach var="room" items="${rooms}">
                        <tr>
                            <td><strong><c:out value="${room.code}"/></strong></td>
                            <td><span class="tag"><c:out value="${room.phase}"/></span></td>
                            <td><c:out value="${room.hostId}"/></td>
                            <td>${room.players} / ${room.maxPlayers}</td>
                            <td>${room.boardSize} &times; ${room.boardSize}</td>
                            <td>${room.round} / ${room.maxRounds}</td>
                            <td>
                                <c:if test="${room.phase != 'FINISHED'}">
                                    <form method="post" action="<c:url value='/admin/rooms/${room.code}/end'/>">
                                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                        <button type="submit">End</button>
                                    </form>
                                </c:if>
                            </td>
                        </tr>
                    </c:forEach>
                </table>
            </c:otherwise>
        </c:choose>
    </section>

    <section>
        <h2>Top players</h2>
        <c:choose>
            <c:when test="${empty players}"><p class="empty">No games recorded yet.</p></c:when>
            <c:otherwise>
                <table>
                    <tr><th>#</th><th>Player</th><th>Rating</th><th>Games</th><th>Wins</th><th>Tiles</th></tr>
                    <c:forEach var="p" items="${players}">
                        <tr>
                            <td>${p.rank}</td>
                            <td><c:out value="${p.username}"/></td>
                            <td>${p.rating}</td>
                            <td>${p.gamesPlayed}</td>
                            <td>${p.wins}</td>
                            <td>${p.totalTiles}</td>
                        </tr>
                    </c:forEach>
                </table>
            </c:otherwise>
        </c:choose>
    </section>

    <section>
        <h2>Recent matches</h2>
        <c:choose>
            <c:when test="${empty matches}"><p class="empty">No matches yet.</p></c:when>
            <c:otherwise>
                <table>
                    <tr><th>ID</th><th>Room</th><th>Finished</th><th>Moves</th><th>Players (score)</th><th>Replay</th></tr>
                    <c:forEach var="m" items="${matches}">
                        <tr>
                            <td>${m.id}</td>
                            <td><c:out value="${m.roomCode}"/></td>
                            <td><c:out value="${m.finishedAt}"/></td>
                            <td>${m.totalMoves}<c:if test="${m.endedEarly}"> (ended early)</c:if></td>
                            <td>
                                <c:forEach var="pl" items="${m.players}" varStatus="s">
                                    <c:if test="${pl.placement == 1}">&#9733; </c:if><c:out value="${pl.displayName}"/> (${pl.score})<c:if test="${!s.last}">, </c:if>
                                </c:forEach>
                            </td>
                            <td><a href="<c:url value='/api/leaderboard/matches/${m.id}/replay.xml'/>">XML</a></td>
                        </tr>
                    </c:forEach>
                </table>
            </c:otherwise>
        </c:choose>
    </section>
</main>
</body>
</html>
