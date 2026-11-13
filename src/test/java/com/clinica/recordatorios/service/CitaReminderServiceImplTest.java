package com.clinica.recordatorios.service;

import com.clinica.recordatorios.channel.ReminderChannel;
import com.clinica.recordatorios.channel.ReminderChannelFactory;
import com.clinica.recordatorios.domain.CanalNotificacion;
import com.clinica.recordatorios.domain.Cita;
import com.clinica.recordatorios.repository.CitaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CitaReminderServiceImplTest {
    @Mock private CitaRepository citaRepository;
    @Mock private ReminderChannelFactory reminderChannelFactory;
    @Mock private ReminderChannel reminderChannelMock;
    @Mock private ReminderChannel canalUrgenteMock;

    // Fake provisto por el JDK: conserva el comportamiento real del reloj.
    private final Clock clockFijo = Clock.fixed(
        Instant.parse("2026-08-10T09:00:00Z"), ZoneId.of("UTC"));
    private CitaReminderServiceImpl citaReminderService;

    @BeforeEach
    void setUp() {
        citaReminderService = new CitaReminderServiceImpl(
            citaRepository, reminderChannelFactory, canalUrgenteMock, clockFijo);
    }

    @Test
    void enviarRecordatorioUrgente_citaExistente_usaCanalUrgenteInyectado() {
        Cita cita = citaUrgente(5L);
        when(citaRepository.findById(5L)).thenReturn(Optional.of(cita));
        citaReminderService.enviarRecordatorioUrgente(5L);
        verify(canalUrgenteMock).enviar(eq(cita), contains("URGENTE"));
        verify(citaRepository).save(argThat(guardada -> guardada == cita && guardada.isRecordatorioEnviado()));
        assertTrue(cita.isRecordatorioEnviado());
        verifyNoInteractions(reminderChannelFactory);
    }

    @Test
    void enviarRecordatorioUrgente_gatewayFalla_noMarcaComoEnviado() {
        Cita cita = citaUrgente(6L);
        when(citaRepository.findById(6L)).thenReturn(Optional.of(cita));
        RuntimeException fallo = new RuntimeException("Gateway de SMS no disponible");
        doThrow(fallo).when(canalUrgenteMock).enviar(any(), anyString());
        assertSame(fallo, assertThrows(RuntimeException.class,
            () -> citaReminderService.enviarRecordatorioUrgente(6L)));
        assertFalse(cita.isRecordatorioEnviado());
        verify(citaRepository, never()).save(any());
    }

    @Test
    void enviarRecordatorioUrgente_citaInexistente_lanzaCitaNoEncontradaException() {
        when(citaRepository.findById(99L)).thenReturn(Optional.empty());
        CitaNoEncontradaException error = assertThrows(CitaNoEncontradaException.class,
            () -> citaReminderService.enviarRecordatorioUrgente(99L));
        assertEquals("Cita no encontrada: 99", error.getMessage());
        verifyNoInteractions(canalUrgenteMock);
        verify(citaRepository, never()).save(any());
    }

    private Cita citaUrgente(Long id) {
        return new Cita(id, "Luis Peña", "3001234567",
            LocalDateTime.of(2026, 8, 10, 10, 30), CanalNotificacion.SMS, false);
    }

    @Test
    void enviarRecordatoriosPendientes_citaEnVentana_usaElCanalPreferido() {
        Cita cita = new Cita(1L, "Ana Gómez", "ana@correo.com",
            LocalDateTime.of(2026, 8, 10, 15, 0), CanalNotificacion.EMAIL, false);
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
            .thenReturn(List.of(cita));
        when(reminderChannelFactory.obtenerCanal(CanalNotificacion.EMAIL))
            .thenReturn(reminderChannelMock);

        int enviados = citaReminderService.enviarRecordatoriosPendientes();

        assertEquals(1, enviados);
        verify(reminderChannelFactory).obtenerCanal(CanalNotificacion.EMAIL);
        verify(reminderChannelMock).enviar(cita, "Recordatorio: tiene una cita el 2026-08-10T15:00");
        verify(citaRepository).save(argThat(guardada -> guardada == cita && guardada.isRecordatorioEnviado()));
        assertTrue(cita.isRecordatorioEnviado());
    }

    @Test
    void enviarRecordatoriosPendientes_ventanaDeterministaSegunClockFijo_consultaLosLimitesExactos() {
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
            .thenReturn(List.of());
        citaReminderService.enviarRecordatoriosPendientes();
        ArgumentCaptor<LocalDateTime> desdeCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> hastaCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(citaRepository).findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(
            desdeCaptor.capture(), hastaCaptor.capture());
        assertEquals(LocalDateTime.of(2026, 8, 10, 9, 0), desdeCaptor.getValue());
        assertEquals(LocalDateTime.of(2026, 8, 11, 9, 0), hastaCaptor.getValue());
    }

    @Test
    void enviarRecordatoriosPendientes_sinCitasPendientes_noConsultaLaFactory() {
        when(citaRepository.findByFechaHoraCitaBetweenAndRecordatorioEnviadoFalse(any(), any()))
            .thenReturn(List.of());
        assertEquals(0, citaReminderService.enviarRecordatoriosPendientes());
        verifyNoInteractions(reminderChannelFactory, reminderChannelMock);
        verify(citaRepository, never()).save(any());
    }
}
