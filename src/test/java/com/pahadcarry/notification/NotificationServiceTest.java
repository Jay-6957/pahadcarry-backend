package com.pahadcarry.notification;

import com.pahadcarry.common.PahadCarryException;
import com.pahadcarry.customer.repository.UserRepository;
import com.pahadcarry.driver.model.Driver;
import com.pahadcarry.driver.repository.DriverRepository;
import com.pahadcarry.notification.model.Notification;
import com.pahadcarry.notification.repository.NotificationRepository;
import com.pahadcarry.notification.service.NotificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private DriverRepository driverRepository;
    @Mock
    private FcmClient fcmClient;

    private NotificationService notificationService;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, userRepository, driverRepository, fcmClient);
    }

    @Test
    void persistsAndPushesNotificationToDriverWithToken() {
        Driver driver = mock(Driver.class);
        when(driverRepository.existsById("driver-1")).thenReturn(true);
        when(driverRepository.findById("driver-1")).thenReturn(Optional.of(driver));
        when(driver.getFcmToken()).thenReturn("token-1");
        when(fcmClient.send("token-1", "Title", "Body", "payload")).thenReturn(true);
        when(notificationRepository.save(any(Notification.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Notification result = notificationService.notifyDriver("driver-1", "Title", "Body", "payload");

        assertThat(result.getRecipientType()).isEqualTo("DRIVER");
        assertThat(result.getSentAt()).isNotNull();
        verify(fcmClient).send("token-1", "Title", "Body", "payload");
        verify(notificationRepository, times(2)).save(any(Notification.class));
    }

    @Test
    void skipsUnknownAutomaticRecipientBeforeSaving() {
        when(userRepository.existsById("missing-user")).thenReturn(false);

        assertThat(notificationService.notifyUser("missing-user", "Title", "Body", null)).isNull();

        verify(notificationRepository, never()).save(any(Notification.class));
        verifyNoInteractions(fcmClient);
    }

    @Test
    void rejectsUnknownRecipientForExplicitSend() {
        when(userRepository.existsById("missing-user")).thenReturn(false);

        assertThatThrownBy(() -> notificationService.createAndSend(
                "USER", "missing-user", "Title", "Body", null))
                .isInstanceOf(PahadCarryException.class)
                .hasMessage("Notification recipient not found");
    }
}
