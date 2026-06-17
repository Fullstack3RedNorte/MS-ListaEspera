package cl.rednorte.ms_lista_espera.controller;

import cl.rednorte.ms_lista_espera.config.SecurityConfig;
import cl.rednorte.ms_lista_espera.dto.request.CambiarEstadoRequest;
import cl.rednorte.ms_lista_espera.dto.request.CrearSolicitudRequest;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudDetalleResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudResponse;
import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.enums.NivelUrgencia;
import cl.rednorte.ms_lista_espera.service.SolicitudService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(SolicitudController.class)
@Import(SecurityConfig.class)
class SolicitudControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private SolicitudService solicitudService;

    @Test
    @WithMockUser
    void crear_requestValido_debeRetornar201() throws Exception {
        CrearSolicitudRequest request = new CrearSolicitudRequest();
        request.setRutPaciente("12345678-9");
        request.setEspecialidadId(1L);
        request.setDiagnostico("Dolor torácico");
        request.setEsGES(false);
        request.setNivelUrgencia(NivelUrgencia.ELECTIVA);
        request.setEsVulnerable(false);

        SolicitudResponse response = new SolicitudResponse();
        response.setId(1L);
        response.setEstado(EstadoSolicitud.EN_ESPERA);

        when(solicitudService.crear(any(), anyString())).thenReturn(response);

        mockMvc.perform(post("/solicitudes")
                .with(jwt().jwt(j -> j.claim("rut", "11111111-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("EN_ESPERA"));
    }

    @Test
    @WithMockUser
    void obtenerDetalle_idExistente_debeRetornar200() throws Exception {
        SolicitudDetalleResponse detalle = new SolicitudDetalleResponse();
        detalle.setId(1L);
        detalle.setRutPaciente("12345678-9");
        detalle.setEstado(EstadoSolicitud.EN_ESPERA);

        when(solicitudService.obtenerDetalle(1L)).thenReturn(detalle);

        mockMvc.perform(get("/solicitudes/1")
                .with(jwt()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.rutPaciente").value("12345678-9"));
    }

    @Test
    @WithMockUser
    void cambiarEstado_requestValido_debeRetornar200() throws Exception {
        CambiarEstadoRequest request = new CambiarEstadoRequest();
        request.setNuevoEstado(EstadoSolicitud.CITADO);
        request.setFechaCita(LocalDateTime.now().plusDays(5));

        SolicitudDetalleResponse detalle = new SolicitudDetalleResponse();
        detalle.setId(1L);
        detalle.setEstado(EstadoSolicitud.CITADO);

        when(solicitudService.cambiarEstado(eq(1L), any(), anyString())).thenReturn(detalle);

        mockMvc.perform(patch("/solicitudes/1/estado")
                .with(jwt().jwt(j -> j.claim("rut", "11111111-1")))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CITADO"));
    }
}