package cl.rednorte.ms_lista_espera.service;

import cl.rednorte.ms_lista_espera.dto.response.EspecialidadResponse;
import java.util.List;

public interface EspecialidadService {

    List<EspecialidadResponse> listarActivas();
}
