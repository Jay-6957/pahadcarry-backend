package com.pahadcarry.notification;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class FcmClient {

    /**
     * No-op FCM sender for now. Logs and returns success.
     * Replace with real FCM integration when credentials are available.
     */
    public boolean send(String token, String title, String body, String payload) {
        if (token == null || token.isBlank()) {
            log.info("FCM send skipped: no token provided");
            return false;
        }
        log.info("FCM send simulated -> token={}, title={}, body={}, payload={}", token, title, body, payload);
        return true;
    }
}
