package cl.rednorte.ms_lista_espera.repository;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.model.entity.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    Page<Solicitud> findByEspecialidadId(Long especialidadId, Pageable pageable);

    Page<Solicitud> findByEstado(EstadoSolicitud estado, Pageable pageable);

    Page<Solicitud> findByRutPaciente(String rutPaciente, Pageable pageable);

    Page<Solicitud> findByEspecialidadIdAndEstado(
        Long especialidadId,
        EstadoSolicitud estado,
        Pageable pageable
    );

    @Query("SELECT s FROM Solicitud s WHERE " +
           "(:especialidadId IS NULL OR s.especialidad.id = :especialidadId) AND " +
           "(:estado IS NULL OR s.estado = :estado) AND " +
           "(:rutPaciente IS NULL OR s.rutPaciente = :rutPaciente)")
    Page<Solicitud> findByFiltros(
        @Param("especialidadId") Long especialidadId,
        @Param("estado") EstadoSolicitud estado,
        @Param("rutPaciente") String rutPaciente,
        Pageable pageable
    );

    // ────────────────────────────────────────────────────────────
    // Consultas para KPIs del dashboard — TODAS aceptan filtro
    // opcional de especialidad.
    // ────────────────────────────────────────────────────────────

    @Query("SELECT COUNT(s) FROM Solicitud s WHERE " +
           "s.estado = :estado AND " +
           "(:especialidadId IS NULL OR s.especialidad.id = :especialidadId)")
    long contarPorEstadoConFiltro(
        @Param("estado") EstadoSolicitud estado,
        @Param("especialidadId") Long especialidadId
    );

    @Query("SELECT COUNT(s) FROM Solicitud s WHERE " +
           "s.fechaRegistro >= :desde AND s.fechaRegistro < :hasta AND " +
           "(:especialidadId IS NULL OR s.especialidad.id = :especialidadId)")
    long contarRegistradasEnRango(
        @Param("desde") LocalDateTime desde,
        @Param("hasta") LocalDateTime hasta,
        @Param("especialidadId") Long especialidadId
    );

    /**
     * Backlog: EN_ESPERA con fechaRegistro anterior al límite.
     * Es una foto del estado actual, no depende del rango.
     */
    @Query("SELECT COUNT(s) FROM Solicitud s WHERE " +
           "s.estado = cl.rednorte.ms_lista_espera.enums.EstadoSolicitud.EN_ESPERA AND " +
           "s.fechaRegistro < :limite AND " +
           "(:especialidadId IS NULL OR s.especialidad.id = :especialidadId)")
    long contarBacklogMayor30Dias(
        @Param("limite") LocalDateTime limite,
        @Param("especialidadId") Long especialidadId
    );

    /**
     * Solicitudes activas por especialidad. Si :especialidadId es null
     * agrupa por todas; si viene, solo muestra esa (útil como control).
     */
    @Query("SELECT s.especialidad.nombre AS especialidad, COUNT(s) AS total " +
           "FROM Solicitud s " +
           "WHERE s.estado IN (cl.rednorte.ms_lista_espera.enums.EstadoSolicitud.EN_ESPERA, " +
           "                   cl.rednorte.ms_lista_espera.enums.EstadoSolicitud.CITADO) AND " +
           "      (:especialidadId IS NULL OR s.especialidad.id = :especialidadId) " +
           "GROUP BY s.especialidad.nombre " +
           "ORDER BY COUNT(s) DESC")
    List<Object[]> conteoActivasPorEspecialidad(
        @Param("especialidadId") Long especialidadId
    );

    @Query("SELECT s.prioridad, COUNT(s) FROM Solicitud s " +
           "WHERE s.estado IN (cl.rednorte.ms_lista_espera.enums.EstadoSolicitud.EN_ESPERA, " +
           "                   cl.rednorte.ms_lista_espera.enums.EstadoSolicitud.CITADO, " +
           "                   cl.rednorte.ms_lista_espera.enums.EstadoSolicitud.ATENDIDO, " +
           "                   cl.rednorte.ms_lista_espera.enums.EstadoSolicitud.AUSENTE) AND " +
           "      (:especialidadId IS NULL OR s.especialidad.id = :especialidadId) " +
           "GROUP BY s.prioridad")
    List<Object[]> conteoActivasPorPrioridad(
        @Param("especialidadId") Long especialidadId
    );
}
