package cl.rednorte.ms_lista_espera.dto.request;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CambiarEstadoRequest {

    @NotNull(message = "El nuevo estado es obligatorio")
    private EstadoSolicitud nuevoEstado;

    private String motivo;
}
