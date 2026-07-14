package com.example.game.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.springframework.stereotype.Service;

@Service
public class MatchService {

    private static class MatchRequest {
        int userId;
        String username;
        int assistantId;

        MatchRequest(int userId, String username, int assistantId) {
            this.userId = userId;
            this.username = username;
            this.assistantId = assistantId;
        }
    }

    private final ConcurrentLinkedQueue<MatchRequest> queue = new ConcurrentLinkedQueue<>();
    private int nextRoomId = 10000;

    private final ConcurrentHashMap<Integer, Map<String, Object>> resultMap = new ConcurrentHashMap<>();

    public Map<String, Object> join(int userId, String username, int assistantId) {
        // 先看看有没有人在等
        MatchRequest opponent = queue.poll();
        if (opponent != null) {
            int roomId = nextRoomId++;

            // 存结果给先排队的人查
            resultMap.put(opponent.userId, Map.of(
                "matched", true, "roomId", roomId, "role", "host",
                "opponentUserId", userId, "opponentUsername", username,
                "opponentAssistantId", assistantId
            ));
            resultMap.put(userId, Map.of(
                "matched", true, "roomId", roomId, "role", "guest",
                "opponentUserId", opponent.userId, "opponentUsername", opponent.username,
                "opponentAssistantId", opponent.assistantId
            ));

            return resultMap.get(userId);
        }
        queue.add(new MatchRequest(userId, username, assistantId));
        return Map.of("success", true, "matched", false, "message", "正在连接中...");
    }

    public Map<String, Object> checkStatus(int userId) {
        Map<String, Object> result = resultMap.remove(userId);
        return result != null ? result : Map.of("matched", false);
    }

    public Map<String, Object> cancel(int userId) {
        queue.removeIf(r -> r.userId == userId);
        resultMap.remove(userId);
        return Map.of("success", true);
    }
}