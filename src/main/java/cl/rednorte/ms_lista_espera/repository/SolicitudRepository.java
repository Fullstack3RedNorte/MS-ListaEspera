package cl.rednorte.ms_lista_espera.repository;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.model.entity.Solicitud;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}