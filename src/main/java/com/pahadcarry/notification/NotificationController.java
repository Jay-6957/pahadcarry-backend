package com.pahadcarry.notification;

import com.pahadcarry.common.ApiResponse;
import com.pahadcarry.notification.model.Notification;
import com.pahadcarry.notification.service.NotificationService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping("/send")
    @PreAuthorize("hasAnyAuthority('ROLE_OPS','ROLE_SUPER_ADMIN')")
    public ResponseEntity<ApiResponse<Notification>> sendNotification(@Valid @RequestBody NotificationRequest req) {
        Notification n = notificationService.createAndSend(req.getRecipientType(), req.getRecipientId(), req.getTitle(), req.getBody(), req.getPayload());
        return ResponseEntity.ok(ApiResponse.ok(n));
    }

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<Notification>>> myNotifications(Authentication auth) {
        String id = (String) auth.getPrincipal();
        List<Notification> list = notificationService.listForRecipient(id);
        return ResponseEntity.ok(ApiResponse.ok(list));
    }

    @Data
    public static class NotificationRequest {
        @NotBlank
        private String recipientType;
        @NotBlank
        private String recipientId;
        @NotBlank
        private String title;
        private String body;
        private String payload;
    }
}
