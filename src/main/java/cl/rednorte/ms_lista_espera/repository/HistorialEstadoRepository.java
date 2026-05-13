package cl.rednorte.ms_lista_espera.repository;

import cl.rednorte.ms_lista_espera.model.entity.HistorialEstado;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface HistorialEstadoRepository extends JpaRepository<HistorialEstado, Long> {

    List<HistorialEstado> findBySolicitudIdOrderByFechaCambioAsc(Long solicitudId);
}