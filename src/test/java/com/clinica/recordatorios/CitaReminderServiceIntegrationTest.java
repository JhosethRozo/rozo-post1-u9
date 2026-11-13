package com.clinica.recordatorios;

import com.clinica.recordatorios.channel.gateway.NotificadorSmsGateway;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import com.clinica.recordatorios.service.CitaReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SpringBootTest
class CitaReminderServiceIntegrationTest {
    @TestConfiguration
    static class ClockDeterministaConfig {
        @Bean
        @Primary
        public Clock clockFijo() {
            return Clock.fixed(Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"));
        }
    }

    @Autowired private CitaReminderService citaReminderService;
    @Autowired private CitaRepository citaRepository;
    @MockBean private NotificadorSmsGateway notificadorSmsGateway;

    @BeforeEach
    void limpiarDatos() { citaRepository.deleteAll(); }

    @Test
    void enviarRecordatoriosPendientes_contextoCompletoDeSpring_persisteYNotificaCitasEnVentana() {
        Cita cita = citaRepository.save(new Cita(null, "Carla Ríos", "3011112222",
            LocalDateTime.of(2026, 8, 10, 18, 0), CanalNotificacion.SMS, false));

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(1, enviados);
        verify(notificadorSmsGateway).enviarSms("3011112222", "Recordatorio: tiene una cita el 2026-08-10T18:00");
        assertTrue(citaRepository.findById(cita.getId()).orElseThrow().isRecordatorioEnviado());
        assertEquals(0, citaReminderService.enviarRecordatoriosPendientes());
        verifyNoMoreInteractions(notificadorSmsGateway);
    }

    @Test
    void enviarRecordatorioUrgente_gatewayFalla_conservaEstadoPendienteEnBaseDeDatos() {
        Cita cita = citaRepository.save(new Cita(null, "Marta Ruiz", "3009876543",
            LocalDateTime.of(2026, 8, 10, 10, 30), CanalNotificacion.SMS, false));
        doThrow(new RuntimeException("Gateway no disponible"))
            .when(notificadorSmsGateway).enviarSms(anyString(), anyString());

        assertThrows(RuntimeException.class, () -> citaReminderService.enviarRecordatorioUrgente(cita.getId()));

        assertFalse(citaRepository.findById(cita.getId()).orElseThrow().isRecordatorioEnviado());
    }
}
