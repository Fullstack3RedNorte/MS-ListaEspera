package cl.rednorte.ms_lista_espera.service;

import cl.rednorte.ms_lista_espera.dto.request.CrearSolicitudRequest;
import cl.rednorte.ms_lista_espera.dto.request.CambiarEstadoRequest;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudDetalleResponse;
import cl.rednorte.ms_lista_espera.dto.response.PageResponse;
import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import org.springframework.data.domain.Pageable;

public interface SolicitudService {

    SolicitudResponse crear(CrearSolicitudRequest request, String rutFuncionario);

    PageResponse<SolicitudResponse> listar(
        Long especialidadId,
        EstadoSolicitud estado,
        String rutPaciente,
        Pageable pageable
    );

    SolicitudDetalleResponse obtenerDetalle(Long id);

    SolicitudDetalleResponse cambiarEstado(Long id, CambiarEstadoRequest request, String rutFuncionario);
}
