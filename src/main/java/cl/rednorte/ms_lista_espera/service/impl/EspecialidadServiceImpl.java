package cl.rednorte.ms_lista_espera.service.impl;

import cl.rednorte.ms_lista_espera.dto.response.EspecialidadResponse;
import cl.rednorte.ms_lista_espera.model.entity.Especialidad;
import cl.rednorte.ms_lista_espera.repository.EspecialidadRepository;
import cl.rednorte.ms_lista_espera.service.EspecialidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class EspecialidadServiceImpl implements EspecialidadService {

    private final EspecialidadRepository especialidadRepository;

    @Override
    public List<EspecialidadResponse> listarActivas() {
        return especialidadRepository.findByActivoTrue()
                .stream()
                .map(this::mapToResponse)
                .toList();
    }

    private EspecialidadResponse mapToResponse(Especialidad especialidad) {
        EspecialidadResponse response = new EspecialidadResponse();
        response.setId(especialidad.getId());
        response.setNombre(especialidad.getNombre());
        response.setDescripcion(especialidad.getDescripcion());
        return response;
    }
}