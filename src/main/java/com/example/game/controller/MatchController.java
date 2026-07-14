package com.example.game.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.game.service.MatchService;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/join")
    public ResponseEntity<Map<String, Object>> join(@RequestBody Map<String, Object> body) {
        int userId = (int) body.get("userId");
        String username = (String) body.get("username");
        int assistantId = (int) body.getOrDefault("assistantId", 30086009);
        return ResponseEntity.ok(matchService.join(userId, username, assistantId));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Map<String, Object>> cancel(@RequestBody Map<String, Object> body) {
        int userId = (int) body.get("userId");
        return ResponseEntity.ok(matchService.cancel(userId));
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> status(@RequestParam int userId) {
        return ResponseEntity.ok(matchService.checkStatus(userId));
    }
}