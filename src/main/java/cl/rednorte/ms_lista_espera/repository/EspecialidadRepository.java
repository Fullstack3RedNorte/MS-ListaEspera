package cl.rednorte.ms_lista_espera.repository;


import cl.rednorte.ms_lista_espera.model.entity.Especialidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface EspecialidadRepository extends JpaRepository<Especialidad, Long> {

    List<Especialidad> findByActivoTrue();
}