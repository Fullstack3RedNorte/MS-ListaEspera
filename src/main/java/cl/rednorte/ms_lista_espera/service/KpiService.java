package cl.rednorte.ms_lista_espera.service;

import cl.rednorte.ms_lista_espera.dto.response.KpisResponse;

import java.time.LocalDate;

/**
 * Servicio de KPIs para el dashboard médico con filtros opcionales.
 *
 * @param especialidadId id de especialidad a filtrar; null = todas
 * @param fechaDesde     inicio del rango (inclusive); null = últimos 30 días
 * @param fechaHasta     fin del rango (exclusivo);   null = hoy
 */
public interface KpiService {
    KpisResponse obtenerKpis(Long especialidadId,
                             LocalDate fechaDesde,
                             LocalDate fechaHasta);
}
