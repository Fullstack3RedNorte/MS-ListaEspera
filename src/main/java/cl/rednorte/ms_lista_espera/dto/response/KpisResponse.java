package cl.rednorte.ms_lista_espera.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * Respuesta agregada del endpoint GET /solicitudes/kpis con filtros.
 *
 * Los KPIs "en rango" se calculan sobre la ventana [fechaDesde, fechaHasta].
 * Los KPIs "snapshot" (enEspera, citadas, backlog) reflejan el estado
 * actual y solo se ven afectados por el filtro de especialidad.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KpisResponse {

    // ─── KPIs snapshot (estado actual) ───────────────────────
    private long totalActivas;              // EN_ESPERA + CITADO
    private long enEspera;
    private long citadas;
    private long backlogMas30Dias;

    // ─── KPIs sobre el rango [fechaDesde, fechaHasta) ────────
    private long atendidasEnRango;
    private long nuevasEnRango;
    private Double tiempoPromedioEsperaDias; // EN_ESPERA → CITADO en rango
    private Double tasaAusentismo;           // % en rango

    // ─── Series para gráficos ────────────────────────────────
    private List<ConteoEspecialidadResponse> porEspecialidad;
    private ConteoPrioridadResponse porPrioridad;

    // ─── Eco del filtro aplicado ─────────────────────────────
    private FiltroAplicado filtroAplicado;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConteoPrioridadResponse {
        private long p1; // GES
        private long p2; // URGENTE
        private long p3; // VULNERABLE
        private long p4; // ELECTIVA
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FiltroAplicado {
        private Long especialidadId;
        private LocalDate fechaDesde;
        private LocalDate fechaHasta;
    }
}
