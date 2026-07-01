package cl.rednorte.ms_lista_espera.repository;

import cl.rednorte.ms_lista_espera.model.entity.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    List<HistorialEstado> findBySolicitudIdOrderByFechaCambioAsc(Long solicitudId);

    // ────────────────────────────────────────────────────────────
    // Consultas para KPIs de tiempos y ausentismo, en un rango
    // [:desde, :hasta) y con filtro opcional de especialidad.
    // ────────────────────────────────────────────────────────────

    /**
     * Tiempo promedio (en horas) entre la creación de la solicitud y su
     * transición a CITADO, para transiciones que ocurrieron en el rango.
     * El servicio divide entre 24 para obtener días con 1 decimal.
     */
    @Query(value =
        "SELECT AVG(TIMESTAMPDIFF(HOUR, s.fecha_registro, h.fecha_cambio)) " +
        "FROM historial_estados h " +
        "JOIN solicitudes s ON s.id = h.solicitud_id " +
        "WHERE h.estado_nuevo = 'CITADO' " +
        "  AND h.fecha_cambio >= :desde " +
        "  AND h.fecha_cambio <  :hasta " +
        "  AND (:especialidadId IS NULL OR s.especialidad_id = :especialidadId)",
        nativeQuery = true)
    Double promedioHorasEsperaHastaCitado(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("especialidadId") Long especialidadId
    );

    /**
     * Cuenta transiciones a un estado dado, en el rango, opcionalmente
     * filtradas por especialidad de la solicitud.
     */
    @Query(value =
        "SELECT COUNT(*) " +
        "FROM historial_estados h " +
        "JOIN solicitudes s ON s.id = h.solicitud_id " +
        "WHERE h.estado_nuevo = :estado " +
        "  AND h.fecha_cambio >= :desde " +
        "  AND h.fecha_cambio <  :hasta " +
        "  AND (:especialidadId IS NULL OR s.especialidad_id = :especialidadId)",
        nativeQuery = true)
    long contarTransicionesEnRango(
        @Param("estado") String estado,
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("especialidadId") Long especialidadId
    );
}
