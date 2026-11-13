# Recordatorios Citas Service

Post-contenido Unidad 9 — Patrones de Diseño de Software. Java 21, Spring Boot 3.3 y Maven 3.9+.

## Decisiones de diseño

Estas decisiones se registran antes de implementar la suite unitaria.

| Colaborador | Elección | Justificación |
| --- | --- | --- |
| CitaRepository | Mock | Controlar la consulta con stubbing y verificar que save recibe exactamente una cita marcada como enviada. Un stub solamente proporcionaría respuestas. |
| ReminderChannelFactory y ReminderChannel | Mock | Verificar obtenerCanal con el canal preferido y enviar con la cita y el mensaje; interesa la interacción, no solo un retorno prefijado. |
| Clock | Fake mediante Clock.fixed | Implementación completa del JDK que congela el instante y mantiene el comportamiento real de LocalDateTime.now(clock). No se mockean instant ni getZone. |

Strategy permite sustituir el transporte con una interfaz propia. Factory permite observar explícitamente la selección de estrategia. La inyección por constructor permite reemplazar ambos colaboradores sin arrancar Spring.
