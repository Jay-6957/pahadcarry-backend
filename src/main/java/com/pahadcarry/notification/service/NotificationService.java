package com.pahadcarry.notification.service;

import com.pahadcarry.common.ErrorCode;
import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.notification.FcmClient;
import com.pahadcarry.notification.model.Notification;
import com.pahadcarry.notification.repository.NotificationRepository;
import com.pahadcarry.customer.repository.UserRepository;
import com.pahadcarry.driver.repository.DriverRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final DriverRepository driverRepository;
    private final FcmClient fcmClient;

    @Transactional
    public Notification createAndSend(String recipientType, String recipientId, String title, String body, String payload) {
        String normalizedType = normalizeRecipientType(recipientType);
        ensureRecipientExists(normalizedType, recipientId);

        Notification n = Notification.builder()
            .recipientType(normalizedType)
                .recipientId(recipientId)
                .title(title)
                .body(body)
                .payload(payload)
                .createdAt(Instant.now())
                .read(false)
                .build();

        Notification saved = notificationRepository.save(n);

        // attempt push delivery if token present
        String token = null;
        if ("DRIVER".equals(normalizedType)) {
            token = driverRepository.findById(recipientId).map(d -> d.getFcmToken()).orElse(null);
        } else {
            token = userRepository.findById(recipientId).map(u -> u.getFcmToken()).orElse(null);
        }

        boolean sent = fcmClient.send(token, title, body, payload);
        if (sent) {
            saved.setSentAt(Instant.now());
            notificationRepository.save(saved);
        }

        return saved;
    }

    private String normalizeRecipientType(String recipientType) {
        if (recipientType == null) {
            throw PahadCarryException.badRequest(ErrorCode.BAD_REQUEST, "Recipient type must be USER or DRIVER");
        }
        String normalizedType = recipientType.trim().toUpperCase(Locale.ROOT);
        if ("CUSTOMER".equals(normalizedType)) {
            normalizedType = "USER";
        }
        if (!"USER".equals(normalizedType) && !"DRIVER".equals(normalizedType)) {
            throw PahadCarryException.badRequest(ErrorCode.BAD_REQUEST, "Recipient type must be USER or DRIVER");
        }
        return normalizedType;
    }

    private void ensureRecipientExists(String recipientType, String recipientId) {
        boolean exists = "DRIVER".equals(recipientType)
                ? driverRepository.existsById(recipientId)
                : userRepository.existsById(recipientId);
        if (!exists) {
            throw PahadCarryException.notFound(ErrorCode.NOT_FOUND, "Notification recipient not found");
        }
    }

    public List<Notification> listForRecipient(String recipientId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId);
    }

    public Notification notifyUser(String userId, String title, String body, String payload) {
        return notifyBestEffort("USER", userId, title, body, payload);
    }

    public Notification notifyDriver(String driverId, String title, String body, String payload) {
        return notifyBestEffort("DRIVER", driverId, title, body, payload);
    }

    private Notification notifyBestEffort(String recipientType, String recipientId, String title, String body, String payload) {
        try {
            return createAndSend(recipientType, recipientId, title, body, payload);
        } catch (PahadCarryException ex) {
            if (ErrorCode.NOT_FOUND.equals(ex.getErrorCode())) {
                log.warn("Skipping notification: {} recipient {} was not found", recipientType, recipientId);
                return null;
            }
            throw ex;
        }
    }
}
