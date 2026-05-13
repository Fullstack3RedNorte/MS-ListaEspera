package cl.rednorte.ms_lista_espera.service.impl;

import cl.rednorte.ms_lista_espera.dto.request.CambiarEstadoRequest;
import cl.rednorte.ms_lista_espera.dto.request.CrearSolicitudRequest;
import cl.rednorte.ms_lista_espera.dto.response.HistorialEstadoResponse;
import cl.rednorte.ms_lista_espera.dto.response.PageResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudDetalleResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudResponse;
import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.enums.NivelUrgencia;
import cl.rednorte.ms_lista_espera.model.entity.Especialidad;
import cl.rednorte.ms_lista_espera.model.entity.HistorialEstado;
import cl.rednorte.ms_lista_espera.model.entity.Solicitud;
import cl.rednorte.ms_lista_espera.model.entity.TipoVulnerabilidad;
import cl.rednorte.ms_lista_espera.repository.EspecialidadRepository;
import cl.rednorte.ms_lista_espera.repository.HistorialEstadoRepository;
import cl.rednorte.ms_lista_espera.repository.SolicitudRepository;
import cl.rednorte.ms_lista_espera.repository.TipoVulnerabilidadRepository;
import cl.rednorte.ms_lista_espera.service.SolicitudService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class SolicitudServiceImpl implements SolicitudService {

    private final SolicitudRepository solicitudRepository;
    private final EspecialidadRepository especialidadRepository;
    private final TipoVulnerabilidadRepository tipoVulnerabilidadRepository;
    private final HistorialEstadoRepository historialEstadoRepository;

    // Estados que requieren motivo obligatorio
    private static final Set<EstadoSolicitud> ESTADOS_CON_MOTIVO_OBLIGATORIO =
        Set.of(EstadoSolicitud.ANULADO, EstadoSolicitud.DERIVADO);

    @Override
    @Transactional
    public SolicitudResponse crear(CrearSolicitudRequest request, String rutFuncionario) {

        // Buscar especialidad
        Especialidad especialidad = especialidadRepository.findById(request.getEspecialidadId())
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Especialidad no encontrada"));

        // Buscar tipo vulnerabilidad si aplica
        TipoVulnerabilidad tipoVulnerabilidad = null;
        if (Boolean.TRUE.equals(request.getEsVulnerable()) && request.getTipoVulnerabilidadId() != null) {
            tipoVulnerabilidad = tipoVulnerabilidadRepository.findById(request.getTipoVulnerabilidadId())
                    .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Tipo de vulnerabilidad no encontrado"));
        }

        // Crear solicitud
        Solicitud solicitud = new Solicitud();
        solicitud.setRutPaciente(request.getRutPaciente());
        solicitud.setRutFuncionario(rutFuncionario);
        solicitud.setEspecialidad(especialidad);
        solicitud.setDiagnostico(request.getDiagnostico());
        solicitud.setEsGES(request.getEsGES());
        solicitud.setPatologiaGES(request.getPatologiaGES());
        solicitud.setNivelUrgencia(request.getNivelUrgencia());
        solicitud.setEsVulnerable(request.getEsVulnerable());
        solicitud.setTipoVulnerabilidad(tipoVulnerabilidad);
        solicitud.setEstado(EstadoSolicitud.EN_ESPERA);
        solicitud.setPrioridad(calcularPrioridad(request));

        Solicitud guardada = solicitudRepository.save(solicitud);

        // Registrar primer historial
        registrarHistorial(guardada, null, EstadoSolicitud.EN_ESPERA, null, rutFuncionario);

        return mapToResponse(guardada);
    }

    @Override
    public PageResponse<SolicitudResponse> listar(
            Long especialidadId,
            EstadoSolicitud estado,
            String rutPaciente,
            Pageable pageable) {

        Page<Solicitud> page = solicitudRepository.findByFiltros(
            especialidadId, estado, rutPaciente, pageable);

        List<SolicitudResponse> content = page.getContent()
                .stream()
                .map(this::mapToResponse)
                .toList();

        return new PageResponse<>(
            content,
            page.getTotalElements(),
            page.getTotalPages(),
            page.getNumber()
        );
    }

    @Override
    public SolicitudDetalleResponse obtenerDetalle(Long id) {
        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        return mapToDetalleResponse(solicitud);
    }

    @Override
    @Transactional
    public SolicitudDetalleResponse cambiarEstado(
            Long id,
            CambiarEstadoRequest request,
            String rutFuncionario) {

        Solicitud solicitud = solicitudRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Solicitud no encontrada"));

        // Validar transición
        if (!esTransicionValida(solicitud.getEstado(), request.getNuevoEstado())) {
            throw new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "Transición no válida de " + solicitud.getEstado() + " a " + request.getNuevoEstado());
        }

        // Validar motivo obligatorio
        if (ESTADOS_CON_MOTIVO_OBLIGATORIO.contains(request.getNuevoEstado()) &&
            (request.getMotivo() == null || request.getMotivo().isBlank())) {
            throw new ResponseStatusException(
                HttpStatus.UNPROCESSABLE_ENTITY,
                "El motivo es obligatorio para el estado " + request.getNuevoEstado());
        }

        EstadoSolicitud estadoAnterior = solicitud.getEstado();
        solicitud.setEstado(request.getNuevoEstado());
        solicitudRepository.save(solicitud);

        registrarHistorial(solicitud, estadoAnterior, request.getNuevoEstado(),
            request.getMotivo(), rutFuncionario);

        return mapToDetalleResponse(solicitud);
    }

    // MÉTODOS PRIVADOS

    private Integer calcularPrioridad(CrearSolicitudRequest request) {
        if (Boolean.TRUE.equals(request.getEsGES()) ||
            NivelUrgencia.GES.equals(request.getNivelUrgencia())) {
            return 1;
        } else if (NivelUrgencia.URGENTE.equals(request.getNivelUrgencia())) {
            return 2;
        } else if (Boolean.TRUE.equals(request.getEsVulnerable())) {
            return 3;
        } else {
            return 4;
        }
    }

    private boolean esTransicionValida(EstadoSolicitud actual, EstadoSolicitud nuevo) {
        return switch (actual) {
            case EN_ESPERA -> Set.of(
                EstadoSolicitud.CITADO,
                EstadoSolicitud.ANULADO,
                EstadoSolicitud.VENCIDO
            ).contains(nuevo);
            case CITADO -> Set.of(
                EstadoSolicitud.ATENDIDO,
                EstadoSolicitud.AUSENTE,
                EstadoSolicitud.ANULADO,
                EstadoSolicitud.DERIVADO,
                EstadoSolicitud.VENCIDO
            ).contains(nuevo);
            case ATENDIDO -> Set.of(
                EstadoSolicitud.CERRADO,
                EstadoSolicitud.DERIVADO
            ).contains(nuevo);
            case AUSENTE -> Set.of(
                EstadoSolicitud.EN_ESPERA,
                EstadoSolicitud.CERRADO
            ).contains(nuevo);
            default -> false;
        };
    }

    private void registrarHistorial(
            Solicitud solicitud,
            EstadoSolicitud estadoAnterior,
            EstadoSolicitud estadoNuevo,
            String motivo,
            String rutUsuario) {

        HistorialEstado historial = new HistorialEstado();
        historial.setSolicitud(solicitud);
        historial.setEstadoAnterior(estadoAnterior);
        historial.setEstadoNuevo(estadoNuevo);
        historial.setMotivo(motivo);
        historial.setRutUsuarioResponsable(rutUsuario);
        historialEstadoRepository.save(historial);
    }

    private SolicitudResponse mapToResponse(Solicitud solicitud) {
        SolicitudResponse response = new SolicitudResponse();
        response.setId(solicitud.getId());
        response.setRutPaciente(solicitud.getRutPaciente());
        response.setEspecialidad(solicitud.getEspecialidad().getNombre());
        response.setPrioridad(solicitud.getPrioridad());
        response.setEstado(solicitud.getEstado());
        response.setFechaRegistro(solicitud.getFechaRegistro());
        return response;
    }

    private SolicitudDetalleResponse mapToDetalleResponse(Solicitud solicitud) {
        SolicitudDetalleResponse response = new SolicitudDetalleResponse();
        response.setId(solicitud.getId());
        response.setRutPaciente(solicitud.getRutPaciente());
        response.setRutFuncionario(solicitud.getRutFuncionario());
        response.setEspecialidad(solicitud.getEspecialidad().getNombre());
        response.setDiagnostico(solicitud.getDiagnostico());
        response.setEsGES(solicitud.getEsGES());
        response.setPatologiaGES(solicitud.getPatologiaGES());
        response.setNivelUrgencia(solicitud.getNivelUrgencia());
        response.setEsVulnerable(solicitud.getEsVulnerable());
        response.setTipoVulnerabilidad(
            solicitud.getTipoVulnerabilidad() != null
                ? solicitud.getTipoVulnerabilidad().getNombre()
                : null
        );
        response.setPrioridad(solicitud.getPrioridad());
        response.setEstado(solicitud.getEstado());
        response.setFechaRegistro(solicitud.getFechaRegistro());
        response.setFechaActualizacion(solicitud.getFechaActualizacion());

        List<HistorialEstadoResponse> historial = historialEstadoRepository
                .findBySolicitudIdOrderByFechaCambioAsc(solicitud.getId())
                .stream()
                .map(this::mapToHistorialResponse)
                .toList();

        response.setHistorial(historial);
        return response;
    }

    private HistorialEstadoResponse mapToHistorialResponse(HistorialEstado historial) {
        HistorialEstadoResponse response = new HistorialEstadoResponse();
        response.setEstadoAnterior(historial.getEstadoAnterior());
        response.setEstadoNuevo(historial.getEstadoNuevo());
        response.setMotivo(historial.getMotivo());
        response.setFechaCambio(historial.getFechaCambio());
        response.setRutUsuarioResponsable(historial.getRutUsuarioResponsable());
        return response;
    }
}