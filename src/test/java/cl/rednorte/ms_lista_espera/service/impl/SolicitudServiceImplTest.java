package cl.rednorte.ms_lista_espera.service.impl;

import cl.rednorte.ms_lista_espera.dto.request.CambiarEstadoRequest;
import cl.rednorte.ms_lista_espera.dto.request.CrearSolicitudRequest;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudDetalleResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudResponse;
import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.enums.NivelUrgencia;
import cl.rednorte.ms_lista_espera.model.entity.Especialidad;
import cl.rednorte.ms_lista_espera.model.entity.Solicitud;
import cl.rednorte.ms_lista_espera.repository.EspecialidadRepository;
import cl.rednorte.ms_lista_espera.repository.HistorialEstadoRepository;
import cl.rednorte.ms_lista_espera.repository.SolicitudRepository;
import cl.rednorte.ms_lista_espera.repository.TipoVulnerabilidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SolicitudServiceImplTest {

    @Mock private SolicitudRepository solicitudRepository;
    @Mock private EspecialidadRepository especialidadRepository;
    @Mock private TipoVulnerabilidadRepository tipoVulnerabilidadRepository;
    @Mock private HistorialEstadoRepository historialEstadoRepository;

    @InjectMocks
    private SolicitudServiceImpl solicitudService;

    private Especialidad especialidadMock;
    private Solicitud solicitudMock;

    @BeforeEach
    void setUp() {
        especialidadMock = new Especialidad();
        especialidadMock.setId(1L);
        especialidadMock.setNombre("Cardiología");

        solicitudMock = new Solicitud();
        solicitudMock.setId(1L);
        solicitudMock.setRutPaciente("12345678-9");
        solicitudMock.setRutFuncionario("11111111-1");
        solicitudMock.setEspecialidad(especialidadMock);
        solicitudMock.setEstado(EstadoSolicitud.EN_ESPERA);
        solicitudMock.setPrioridad(4);
    }

    // ──── CREAR ────

    @Test
    void crear_solicitudNormal_debeRetornarResponse() {
        CrearSolicitudRequest request = new CrearSolicitudRequest();
        request.setRutPaciente("12345678-9");
        request.setEspecialidadId(1L);
        request.setDiagnostico("Dolor torácico");
        request.setEsGES(false);
        request.setNivelUrgencia(NivelUrgencia.ELECTIVA);
        request.setEsVulnerable(false);

        when(especialidadRepository.findById(1L)).thenReturn(Optional.of(especialidadMock));
        when(solicitudRepository.save(any())).thenReturn(solicitudMock);
        when(historialEstadoRepository.save(any())).thenReturn(null);

        SolicitudResponse response = solicitudService.crear(request, "11111111-1");

        assertThat(response).isNotNull();
        assertThat(response.getEstado()).isEqualTo(EstadoSolicitud.EN_ESPERA);
        verify(solicitudRepository, times(1)).save(any());
    }

    @Test
    void crear_especialidadNoExiste_debeArrojarNotFound() {
        CrearSolicitudRequest request = new CrearSolicitudRequest();
        request.setEspecialidadId(99L);

        when(especialidadRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudService.crear(request, "11111111-1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Especialidad no encontrada");
    }

    @Test
    void crear_esGES_debeTenerPrioridadUno() {
        CrearSolicitudRequest request = new CrearSolicitudRequest();
        request.setRutPaciente("12345678-9");
        request.setEspecialidadId(1L);
        request.setDiagnostico("Patología GES");
        request.setEsGES(true);
        request.setNivelUrgencia(NivelUrgencia.ELECTIVA);
        request.setEsVulnerable(false);

        Solicitud solicitudGES = new Solicitud();
        solicitudGES.setId(2L);
        solicitudGES.setEspecialidad(especialidadMock);
        solicitudGES.setEstado(EstadoSolicitud.EN_ESPERA);
        solicitudGES.setPrioridad(1);

        when(especialidadRepository.findById(1L)).thenReturn(Optional.of(especialidadMock));
        when(solicitudRepository.save(any())).thenReturn(solicitudGES);
        when(historialEstadoRepository.save(any())).thenReturn(null);

        SolicitudResponse response = solicitudService.crear(request, "11111111-1");

        assertThat(response.getPrioridad()).isEqualTo(1);
    }

    // ──── OBTENER DETALLE ────

    @Test
    void obtenerDetalle_idExistente_debeRetornarDetalle() {
        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudMock));
        when(historialEstadoRepository.findBySolicitudIdOrderByFechaCambioAsc(1L))
            .thenReturn(java.util.List.of());

        SolicitudDetalleResponse detalle = solicitudService.obtenerDetalle(1L);

        assertThat(detalle).isNotNull();
        assertThat(detalle.getRutPaciente()).isEqualTo("12345678-9");
    }

    @Test
    void obtenerDetalle_idNoExistente_debeArrojarNotFound() {
        when(solicitudRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> solicitudService.obtenerDetalle(99L))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Solicitud no encontrada");
    }

    // ──── CAMBIAR ESTADO ────

    @Test
    void cambiarEstado_transicionValidaEnEsperaACitado_debeActualizar() {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setNuevoEstado(EstadoSolicitud.CITADO);
        request.setFechaCita(LocalDateTime.now().plusDays(5));

        Solicitud solicitudActualizada = new Solicitud();
        solicitudActualizada.setId(1L);
        solicitudActualizada.setEspecialidad(especialidadMock);
        solicitudActualizada.setEstado(EstadoSolicitud.CITADO);
        solicitudActualizada.setFechaCita(request.getFechaCita());

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudMock));
        when(solicitudRepository.save(any())).thenReturn(solicitudActualizada);
        when(historialEstadoRepository.save(any())).thenReturn(null);
        when(historialEstadoRepository.findBySolicitudIdOrderByFechaCambioAsc(1L))
            .thenReturn(java.util.List.of());

        SolicitudDetalleResponse response = solicitudService.cambiarEstado(1L, request, "11111111-1");

        assertThat(response.getEstado()).isEqualTo(EstadoSolicitud.CITADO);
    }

    @Test
    void cambiarEstado_transicionInvalida_debeArrojarBadRequest() {
        solicitudMock.setEstado(EstadoSolicitud.CERRADO);
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setNuevoEstado(EstadoSolicitud.CITADO);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudMock));

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1L, request, "11111111-1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("Transición no válida");
    }

    @Test
    void cambiarEstado_anuladoSinMotivo_debeArrojarUnprocessable() {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setNuevoEstado(EstadoSolicitud.ANULADO);
        request.setMotivo(null);

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudMock));

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1L, request, "11111111-1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("motivo es obligatorio");
    }

    @Test
    void cambiarEstado_citadoConFechaPasada_debeArrojarUnprocessable() {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setNuevoEstado(EstadoSolicitud.CITADO);
        request.setFechaCita(LocalDateTime.now().minusDays(1)); // fecha pasada

        when(solicitudRepository.findById(1L)).thenReturn(Optional.of(solicitudMock));

        assertThatThrownBy(() -> solicitudService.cambiarEstado(1L, request, "11111111-1"))
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining("fecha y hora futura");
    }
}