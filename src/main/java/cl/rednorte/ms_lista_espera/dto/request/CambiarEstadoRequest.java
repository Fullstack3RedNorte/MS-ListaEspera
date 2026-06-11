package cl.rednorte.ms_lista_espera.dto.request;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class CambiarEstadoRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoSolicitud nuevoEstado;

    private String motivo;

    // Fecha y hora de la cita. Obligatoria cuando nuevoEstado=CITADO.
    // La validación cruzada se hace en SolicitudServiceImpl.cambiarEstado()
    // porque depende del valor de nuevoEstado.
    private LocalDateTime fechaCita;
}