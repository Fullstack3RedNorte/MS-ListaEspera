package cl.rednorte.ms_lista_espera.controller;

import cl.rednorte.ms_lista_espera.dto.request.CambiarEstadoRequest;
import cl.rednorte.ms_lista_espera.dto.request.CrearSolicitudRequest;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudDetalleResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudResponse;
import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.enums.NivelUrgencia;
import cl.rednorte.ms_lista_espera.service.SolicitudService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class SolicitudControllerTest {

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @Mock private SolicitudService solicitudService;
    @InjectMocks private SolicitudController solicitudController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(solicitudController).build();
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
    }

    @Test
    void crear_requestValido_debeRetornar201() throws Exception {
        CrearSolicitudRequest request = new CrearSolicitudRequest();
        request.setRutPaciente("12345678-9");
        request.setEspecialidadId(1L);
        request.setDiagnostico("Dolor torácico");
        request.setEsGES(false);
        request.setNivelUrgencia(NivelUrgencia.NORMAL);
        request.setEsVulnerable(false);

        SolicitudResponse response = new SolicitudResponse();
        response.setId(1L);
        response.setEstado(EstadoSolicitud.EN_ESPERA);

        when(solicitudService.crear(any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("EN_ESPERA"));
    }

    @Test
    void obtenerDetalle_idExistente_debeRetornar200() throws Exception {
        SolicitudDetalleResponse detalle = new SolicitudDetalleResponse();
        detalle.setId(1L);
        detalle.setRutPaciente("12345678-9");
        detalle.setEstado(EstadoSolicitud.EN_ESPERA);

        when(solicitudService.obtenerDetalle(1L)).thenReturn(detalle);

        mockMvc.perform(get("/solicitudes/1"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rutPaciente").value("12345678-9"));
    }

    @Test
    void cambiarEstado_requestValido_debeRetornar200() throws Exception {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setNuevoEstado(EstadoSolicitud.CITADO);
        request.setFechaCita(LocalDateTime.now().plusDays(5));

        SolicitudDetalleResponse detalle = new SolicitudDetalleResponse();
        detalle.setId(1L);
        detalle.setEstado(EstadoSolicitud.CITADO);

        when(solicitudService.cambiarEstado(eq(1L), any(), anyString())).thenReturn(detalle);

        mockMvc.perform(patch("/solicitudes/1/estado")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CITADO"));
    }
}