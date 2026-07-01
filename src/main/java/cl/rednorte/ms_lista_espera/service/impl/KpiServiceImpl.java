package cl.rednorte.ms_lista_espera.service.impl;

import cl.rednorte.ms_lista_espera.dto.response.ConteoEspecialidadResponse;
import cl.rednorte.ms_lista_espera.dto.response.KpisResponse;
import cl.rednorte.ms_lista_espera.dto.response.KpisResponse.ConteoPrioridadResponse;
import cl.rednorte.ms_lista_espera.dto.response.KpisResponse.FiltroAplicado;
import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.repository.HistorialEstadoRepository;
import cl.rednorte.ms_lista_espera.repository.SolicitudRepository;
import cl.rednorte.ms_lista_espera.service.KpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class KpiServiceImpl implements KpiService {

    private final SolicitudRepository solicitudRepository;
    private final HistorialEstadoRepository historialEstadoRepository;

    @Override
    @Transactional(readOnly = true)
    public KpisResponse obtenerKpis(Long especialidadId,
                                    LocalDate fechaDesde,
                                    LocalDate fechaHasta) {

        // ─── Resolver rango de fechas ────────────────────
        LocalDate hoy = LocalDate.now();
        LocalDate desde = (fechaDesde != null) ? fechaDesde : hoy.minusDays(30);
        LocalDate hasta = (fechaHasta != null) ? fechaHasta : hoy;
        // hasta es EXCLUSIVO en las queries, así que sumamos 1 día para
        // incluir el día completo que el usuario seleccionó como "hasta".
        LocalDateTime desdeDt = desde.atStartOfDay();
        LocalDateTime hastaDt = hasta.plusDays(1).atStartOfDay();

        LocalDateTime hace30Dias = LocalDateTime.now().minusDays(30);

        // ─── Snapshot (no dependen del rango) ────────────
        long enEspera = solicitudRepository
            .contarPorEstadoConFiltro(EstadoSolicitud.EN_ESPERA, especialidadId);
        long citadas = solicitudRepository
            .contarPorEstadoConFiltro(EstadoSolicitud.CITADO, especialidadId);
        long totalActivas = enEspera + citadas;
        long backlog = solicitudRepository
            .contarBacklogMayor30Dias(hace30Dias, especialidadId);

        // ─── En rango ────────────────────────────────────
        long nuevasEnRango = solicitudRepository
            .contarRegistradasEnRango(desdeDt, hastaDt, especialidadId);

        long atendidasEnRango = historialEstadoRepository
            .contarTransicionesEnRango("ATENDIDO", desdeDt, hastaDt, especialidadId);
        long ausentesEnRango = historialEstadoRepository
            .contarTransicionesEnRango("AUSENTE", desdeDt, hastaDt, especialidadId);
        long denomAusentismo = atendidasEnRango + ausentesEnRango;
        Double tasaAusentismo = (denomAusentismo == 0)
            ? null
            : Math.round((ausentesEnRango * 1000.0) / denomAusentismo) / 10.0;

        Double promHoras = historialEstadoRepository
            .promedioHorasEsperaHastaCitado(desdeDt, hastaDt, especialidadId);
        Double promedioDias = (promHoras != null)
            ? Math.round((promHoras / 24.0) * 10.0) / 10.0
            : null;

        // ─── Series para gráficos ────────────────────────
        List<ConteoEspecialidadResponse> porEspecialidad = solicitudRepository
            .conteoActivasPorEspecialidad(especialidadId)
            .stream()
            .map(row -> new ConteoEspecialidadResponse(
                (String) row[0],
                ((Number) row[1]).longValue()))
            .toList();

        ConteoPrioridadResponse porPrioridad = mapearPrioridades(
            solicitudRepository.conteoActivasPorPrioridad(especialidadId));

        return new KpisResponse(
            totalActivas,
            enEspera,
            citadas,
            backlog,
            atendidasEnRango,
            nuevasEnRango,
            promedioDias,
            tasaAusentismo,
            porEspecialidad,
            porPrioridad,
            new FiltroAplicado(especialidadId, desde, hasta)
        );
    }

    private ConteoPrioridadResponse mapearPrioridades(List<Object[]> rows) {
        long p1 = 0, p2 = 0, p3 = 0, p4 = 0;
        for (Object[] row : rows) {
            int prio = ((Number) row[0]).intValue();
            long total = ((Number) row[1]).longValue();
            switch (prio) {
                case 1 -> p1 = total;
                case 2 -> p2 = total;
                case 3 -> p3 = total;
                case 4 -> p4 = total;
                default -> { /* ignore */ }
            }
        }
        return new ConteoPrioridadResponse(p1, p2, p3, p4);
    }
}
