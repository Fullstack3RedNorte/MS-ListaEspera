package cl.rednorte.ms_lista_espera.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Conteo de solicitudes activas agrupadas por especialidad.
 * Usado para el gráfico de barras del dashboard.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConteoEspecialidadResponse {
    private String especialidad;
    private long total;
}
