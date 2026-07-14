package com.example.game.websocket;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ObjectMapper mapper = new ObjectMapper();
    private final Map<Integer, Map<Integer, WebSocketSession>> rooms = new ConcurrentHashMap<>();
    private final Map<Integer, RoomDraft> drafts = new ConcurrentHashMap<>();

    private static final int[][] TURN_RULES = {
        // {turn, playerSide, action, count}
        // playerSide: 0=host, 1=guest; action: 0=pick, 1=ban
        {1, 0, 0, 1},  // 0号位选1个
        {2, 1, 0, 1},  // 1号位选1个
        {3, 0, 1, 1},  // 0号位禁1个
        {4, 1, 1, 1},  // 1号位禁1个
        {5, 1, 0, 2},  // 1号位选2个
        {6, 0, 0, 2},  // 0号位选2个
        {7, 1, 1, 2},  // 1号位禁2个
        {8, 0, 1, 2},  // 0号位禁2个
        {9, 0, 0, 2},  // 0号位选2个
        {10, 1, 0, 2}, // 1号位选2个
    };

    static class RoomDraft {
        int currentTurn = 1;
        List<Integer> hostPicks = new ArrayList<>();
        List<Integer> guestPicks = new ArrayList<>();
        List<Integer> hostBans = new ArrayList<>();
        List<Integer> guestBans = new ArrayList<>();
        List<Integer> tempSelection = new ArrayList<>();
        boolean hostConfirmed = false;
        boolean guestConfirmed = false;
        String hostUsername = "";
        String guestUsername = "";
        List<Integer> hostOrder = new ArrayList<>();
        List<Integer> guestOrder = new ArrayList<>();
        boolean hostPosConfirmed = false;
        boolean guestPosConfirmed = false;
        boolean hostReady = false;
        boolean guestReady = false;
        int hostAssistantId = 30086009;
        int guestAssistantId = 30086009;
        int hostUserId = 0;
        int guestUserId = 0;
        boolean inBattle = false;
        long disconnectedAt = 0;   // 0 表示没断开，>0 表示断开的时间戳
        int disconnectedUserId = 0;
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        Map<String, Object> msg = mapper.readValue(message.getPayload(), Map.class);
        String type = (String) msg.get("type");

        switch (type) {
            case "JOIN_ROOM"     -> handleJoin(session, msg);
            case "CONFIRM_PICK"  -> handleConfirmPick(msg);
            case "CONFIRM_BAN"   -> handleConfirmBan(msg);
            case "CONFIRM_ROSTER" -> handleConfirmRoster(msg);
            case "CONFIRM_POSITION" -> handleConfirmPosition(msg);
            case "READY"           -> handleReady(msg);
            case "LOBBY_READY"   -> handleLobbyReady(msg);
            case "LEAVE_ROOM"    -> handleLeave(msg);
            case "START_GAME"    -> handleStartGame(msg);
        }
    }

    private void handleJoin(WebSocketSession session, Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        int userId = (int) msg.get("userId");
        int side = (int) msg.get("side");
        String username = (String) msg.get("username");
        int assistantId = msg.get("assistantId") != null ? (int) msg.get("assistantId") : 30086009;
        String phase = msg.get("phase") != null ? (String) msg.get("phase") : "game";

        //判断是否是重连玩家
        RoomDraft existing = drafts.get(roomId);
        if (existing != null && existing.disconnectedAt > 0) {
            existing.disconnectedAt = 0;
            existing.disconnectedUserId = 0;
            rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
            broadcast(roomId, Map.of("type", "PLAYER_RECONNECTED", "userId", userId));
            return;
        }

        rooms.computeIfAbsent(roomId, k -> new ConcurrentHashMap<>()).put(userId, session);
        RoomDraft d = drafts.computeIfAbsent(roomId, k -> new RoomDraft());
        if (side == 0) { d.hostUsername = username; d.hostAssistantId = assistantId; d.hostUserId = userId; }
        else { d.guestUsername = username; d.guestAssistantId = assistantId; d.guestUserId = userId; }
        if (rooms.get(roomId).size() == 2) {
            if ("lobby".equals(phase)) {
                if (d.inBattle) {
                    d.hostReady = false;
                    d.guestReady = false;
                    d.inBattle = false;
                    broadcast(roomId, Map.of("type", "BACK_TO_LOBBY",
                        "hostUsername", d.hostUsername, "guestUsername", d.guestUsername,
                        "hostAssistantId", d.hostAssistantId, "guestAssistantId", d.guestAssistantId,
                        "hostUserId", d.hostUserId, "guestUserId", d.guestUserId));
                } else {
                    broadcast(roomId, Map.of("type", "ROOM_JOINED",
                        "hostUsername", d.hostUsername, "guestUsername", d.guestUsername,
                        "hostAssistantId", d.hostAssistantId, "guestAssistantId", d.guestAssistantId,
                        "hostUserId", d.hostUserId, "guestUserId", d.guestUserId));
                }
            } else if (d.currentTurn <= 10) {
                broadcastTurn(roomId);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void handleConfirmPick(Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        int userId = (int) msg.get("userId");
        List<Integer> picks = (List<Integer>) msg.get("picks");
        RoomDraft d = drafts.get(roomId);
        if (d == null) return;

        int[] rule = TURN_RULES[d.currentTurn - 1];
        int playerSide = (int) msg.get("role");
        if (playerSide != rule[1] || rule[2] != 0) return;
        if (picks.size() != rule[3]) return;

        if (playerSide == 0) d.hostPicks.addAll(picks);
        else d.guestPicks.addAll(picks);

        broadcast(roomId, Map.of(
            "type", "TURN_COMPLETE",
            "side", playerSide,
            "action", "pick",
            "roleIds", picks
        ));

        d.currentTurn++;
        d.tempSelection.clear();
        if (d.currentTurn > 10) {
            broadcast(roomId, Map.of("type", "ALL_TURNS_DONE"));
        } else {
            broadcastTurn(roomId);
        }
    }

    @SuppressWarnings("unchecked")
    private void handleConfirmBan(Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        int userId = (int) msg.get("userId");
        List<Integer> bans = (List<Integer>) msg.get("bans");
        RoomDraft d = drafts.get(roomId);
        if (d == null) return;

        int[] rule = TURN_RULES[d.currentTurn - 1];
        int playerSide = (int) msg.get("role");
        if (playerSide != rule[1] || rule[2] != 1) return;
        if (bans.size() != rule[3]) return;

        if (playerSide == 0) d.hostBans.addAll(bans);
        else d.guestBans.addAll(bans);

        broadcast(roomId, Map.of(
            "type", "TURN_COMPLETE",
            "side", playerSide,
            "action", "ban",
            "roleIds", bans
        ));

        d.currentTurn++;
        d.tempSelection.clear();
        if (d.currentTurn > 10) {
            broadcast(roomId, Map.of("type", "ALL_TURNS_DONE"));
        } else {
            broadcastTurn(roomId);
        }
    }

    private void handleConfirmRoster(Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        int userId = (int) msg.get("userId");
        int playerSide = (int) msg.get("role");
        RoomDraft d = drafts.get(roomId);
        if (d == null || d.currentTurn <= 10) return;

        if (playerSide == 0) {
            d.hostConfirmed = true;
        } else {
            d.guestConfirmed = true;
        }
        broadcast(roomId, Map.of("type", "ROSTER_CONFIRMED", "userId", userId));

        if (d.hostConfirmed && d.guestConfirmed) {
            broadcast(roomId, Map.of("type", "PICK_PHASE_COMPLETE",
                "hostPicks", d.hostPicks, "guestPicks", d.guestPicks));
        }
    }

    @SuppressWarnings("unchecked")
    private void handleConfirmPosition(Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        int playerSide = (int) msg.get("role");
        List<Integer> order = (List<Integer>) msg.get("order");
        RoomDraft d = drafts.get(roomId);
        if (d == null) return;

        if (playerSide == 0) {
            d.hostOrder = order;
            d.hostPosConfirmed = true;
        } else {
            d.guestOrder = order;
            d.guestPosConfirmed = true;
        }
        broadcast(roomId, Map.of("type", "POSITION_CONFIRMED", "userId", (int) msg.get("userId")));

        if (d.hostPosConfirmed && d.guestPosConfirmed) {
            List<Integer> merged = new ArrayList<>();
            merged.addAll(d.hostOrder);
            merged.addAll(d.guestOrder);
            broadcast(roomId, Map.of("type", "POSITION_PHASE_COMPLETE",
                "roleIds", merged,
                "hostOrder", d.hostOrder,
                "guestOrder", d.guestOrder));
        }
    }

    private void handleReady(Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        int playerSide = (int) msg.get("role");
        RoomDraft d = drafts.get(roomId);
        if (d == null) return;

        if (playerSide == 0) {
            d.hostReady = true;
        } else {
            d.guestReady = true;
        }
        broadcast(roomId, Map.of("type", "PLAYER_READY", "side", playerSide));

        if (d.hostReady && d.guestReady) {
            d.hostReady = false;
            d.guestReady = false;
            broadcast(roomId, Map.of("type", "BATTLE_BEGIN"));
        }
    }

    private void handleLeave(Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        int userId = (int) msg.get("userId");
        Map<Integer, WebSocketSession> room = rooms.get(roomId);
        if (room != null) {
            room.remove(userId);
            if (room.isEmpty()) {
                rooms.remove(roomId);
                drafts.remove(roomId);
            }
        }
        broadcast(roomId, Map.of("type", "PLAYER_LEFT", "userId", userId));
    }

    private void handleLobbyReady(Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        int playerSide = (int) msg.get("side");
        boolean ready = (boolean) msg.get("ready");
        RoomDraft d = drafts.get(roomId);
        if (d == null) return;
        if (playerSide == 0) d.hostReady = ready;
        else d.guestReady = ready;
        broadcast(roomId, Map.of("type", "LOBBY_READY", "side", playerSide, "ready", ready));
    }

    private void handleStartGame(Map<String, Object> msg) {
        int roomId = (int) msg.get("roomId");
        RoomDraft d = drafts.get(roomId);
        if (d == null) return;
        if (d.hostReady && d.guestReady) {
            d.currentTurn = 1;
            d.hostPicks.clear();
            d.guestPicks.clear();
            d.hostBans.clear();
            d.guestBans.clear();
            d.tempSelection.clear();
            d.hostConfirmed = false;
            d.guestConfirmed = false;
            d.hostOrder.clear();
            d.guestOrder.clear();
            d.hostPosConfirmed = false;
            d.guestPosConfirmed = false;
            d.hostReady = false;
            d.guestReady = false;
            d.inBattle = true;
            broadcast(roomId, Map.of("type", "BATTLE_START"));
        }
    }

    private void broadcastTurn(int roomId) {
        RoomDraft d = drafts.get(roomId);
        if (d == null) return;
        int[] rule = TURN_RULES[d.currentTurn - 1];
        String sideName = rule[1] == 0 ? d.hostUsername : d.guestUsername;
        String actionName = rule[2] == 0 ? "选择" : "禁用";
        Map<String, Object> data = new java.util.HashMap<>();
        data.put("type", "TURN_CHANGE");
        data.put("turn", d.currentTurn);
        data.put("side", rule[1]);
        data.put("action", rule[2]);
        data.put("count", rule[3]);
        data.put("message", "请【" + sideName + "】" + actionName + rule[3] + "个角色");
        broadcast(roomId, data);
    }

    private void broadcast(int roomId, Map<String, Object> data) {
        try {
            Map<Integer, WebSocketSession> room = rooms.get(roomId);
            if (room != null) {
                String json = mapper.writeValueAsString(data);
                for (WebSocketSession s : room.values()) {
                    if (s.isOpen()) {
                        synchronized (s) {                    // ← 加锁
                            if (s.isOpen()) {                 // ← 双重检查
                                s.sendMessage(new TextMessage(json));
                            }
                        }
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        rooms.forEach((roomId, room) -> {
            room.values().remove(session);
            RoomDraft d = drafts.get(roomId);
            if (d == null) return;

            // 找出断开的是谁
            int disconnectedUserId = 0;
            if (d.hostUserId > 0 && !room.containsKey(d.hostUserId)) {
                disconnectedUserId = d.hostUserId;
            } else if (d.guestUserId > 0 && !room.containsKey(d.guestUserId)) {
                disconnectedUserId = d.guestUserId;
            }
            d.disconnectedAt = System.currentTimeMillis();
            d.disconnectedUserId = disconnectedUserId;

            new Thread(() -> {
                try { Thread.sleep(30000); } catch (Exception ex) {}
                if (d.disconnectedAt > 0) {
                    // 30 秒未重连，清理房间
                    if (d.disconnectedUserId > 0) {
                        broadcast(roomId, Map.of("type", "PLAYER_LEFT", "userId", d.disconnectedUserId));
                    }
                    rooms.remove(roomId);
                    drafts.remove(roomId);
                }
            }).start();
        });
    }
}