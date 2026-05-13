package cl.rednorte.ms_lista_espera.dto.response;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class HistorialEstadoResponse {

    private EstadoSolicitud estadoAnterior;
    private EstadoSolicitud estadoNuevo;
    private String motivo;
    private LocalDateTime fechaCambio;
    private String rutUsuarioResponsable;
}
