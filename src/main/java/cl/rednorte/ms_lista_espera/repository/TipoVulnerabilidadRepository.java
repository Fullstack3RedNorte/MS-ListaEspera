package cl.rednorte.ms_lista_espera.repository;

import cl.rednorte.ms_lista_espera.model.entity.TipoVulnerabilidad;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TipoVulnerabilidadRepository extends JpaRepository<TipoVulnerabilidad, Long> {
    
}