package cl.rednorte.ms_lista_espera.controller;

import cl.rednorte.ms_lista_espera.dto.response.KpisResponse;
import cl.rednorte.ms_lista_espera.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/**
 * KPIs del dashboard médico.
 *
 *   GET /solicitudes/kpis
 *     ?especialidadId=1
 *     &fechaDesde=2026-01-01
 *     &fechaHasta=2026-06-30
 *
 * Todos los parámetros son opcionales:
 *   - Sin especialidadId → todas las especialidades
 *   - Sin fechaDesde     → últimos 30 días
 *   - Sin fechaHasta     → hoy
 *
 * Vía BFF: GET /bff/lista-espera/solicitudes/kpis?...
 */
@RestController
@RequestMapping("/solicitudes/kpis")
@RequiredArgsConstructor
public class KpiController {

    private final KpiService kpiService;

    @GetMapping
    public ResponseEntity<KpisResponse> obtenerKpis(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaDesde,
            @RequestParam(required = false)
                @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fechaHasta
    ) {
        return ResponseEntity.ok(
            kpiService.obtenerKpis(especialidadId, fechaDesde, fechaHasta)
        );
    }
}
