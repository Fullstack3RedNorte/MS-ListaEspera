package cl.rednorte.ms_lista_espera.dto.response;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import lombok.Data;
import java.time.LocalDateTime;

@Data
public class SolicitudResponse {

    private Long id;
    private String rutPaciente;
    private String especialidad;
    private Integer prioridad;
    private EstadoSolicitud estado;
    private LocalDateTime fechaRegistro;
}
