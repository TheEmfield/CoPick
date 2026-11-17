package com.copick.alerts;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    @Value("${telegram.bot.token}")
    private String botToken;

    @Value("${telegram.bot.chat-id}")
    private String chatId;

    @PostMapping
    public ResponseEntity<String> receiveAlert(@RequestBody JsonNode payload) {
        try {
            JsonNode alerts = payload.get("alerts");

            if (alerts == null || !alerts.isArray()) {
                return ResponseEntity.badRequest().body("No alerts in payload");
            }

            for (JsonNode alert : alerts) {
                String alertName = alert.path("labels").path("alertname").asText("Unknown");
                String severity = alert.path("labels").path("severity").asText("unknown");
                String summary = alert.path("annotations").path("summary").asText("-");
                String description = alert.path("annotations").path("description").asText("-");

                String message = String.format(
                        "*ALERT: %s*\n\n" +
                                "Severity: *%s*\n" +
                                "Summary: %s\n" +
                                "Description: %s",
                        alertName, severity, summary, description
                );

                sendToTelegram(message);
            }

            return ResponseEntity.ok("OK");
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    private void sendToTelegram(String text) {
        try {
            String url = "https://api.telegram.org/bot" + botToken + "/sendMessage";

            Map<String, Object> body = new HashMap<>();
            body.put("chat_id", chatId);
            body.put("text", text);
            body.put("parse_mode", "Markdown");

            RestTemplate restTemplate = new RestTemplate();
            restTemplate.postForObject(url, body, String.class);

            System.out.println("Telegram message sent: " + text);
        } catch (Exception e) {
            System.err.println("Failed to send Telegram message: " + e.getMessage());
            e.printStackTrace();
        }
    }
}