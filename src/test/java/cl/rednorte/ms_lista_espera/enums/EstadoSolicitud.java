package cl.rednorte.ms_lista_espera.enums;

public enum EstadoSolicitud {
    EN_ESPERA,  // Estado inicial al registrarse
    CITADO,     // Ya tiene una cita programada
    ATENDIDO,   // El proceso de salud se completó
    AUSENTE,    // El paciente no asistió a la cita
    CERRADO,    // Caso finalizado administrativamente
    ANULADO,    // Solicitud cancelada por error o decisión del paciente
    DERIVADO,   // Enviado a otro centro o especialidad
    VENCIDO     // La solicitud superó el tiempo máximo de espera
}
