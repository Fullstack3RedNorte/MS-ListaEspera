package cl.rednorte.ms_lista_espera.controller;
import cl.rednorte.ms_lista_espera.dto.request.CambiarEstadoRequest;
import cl.rednorte.ms_lista_espera.dto.request.CrearSolicitudRequest;
import cl.rednorte.ms_lista_espera.dto.response.PageResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudDetalleResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudResponse;
import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.enums.NivelUrgencia;
import cl.rednorte.ms_lista_espera.service.SolicitudService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SolicitudController.class)
@Import(SolicitudControllerTest.SecurityTestConfig.class)
class SolicitudControllerTest {

    @TestConfiguration
    static class SecurityTestConfig {
        @Bean
        SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
            http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
            return http.build();
        }
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    @MockBean
    private SolicitudService solicitudService;

    @Test
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

        when(solicitudService.crear(any(CrearSolicitudRequest.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/solicitudes")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.estado").value("EN_ESPERA"));
    }

    @Test
    void listar_sinFiltros_debeRetornar200() throws Exception {
        PageResponse<SolicitudResponse> pageResponse = new PageResponse<>(
                Collections.emptyList(), 0L, 0, 0
        );

        when(solicitudService.listar(any(), any(), any(), any())).thenReturn(pageResponse);

        mockMvc.perform(get("/solicitudes"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    void obtenerDetalle_idExistente_debeRetornar200() throws Exception {
        SolicitudDetalleResponse detalle = new SolicitudDetalleResponse();
        detalle.setId(1L);
        detalle.setRutPaciente("12345678-9");
        detalle.setEstado(EstadoSolicitud.EN_ESPERA);

        when(solicitudService.obtenerDetalle(1L)).thenReturn(detalle);

        mockMvc.perform(get("/solicitudes/{id}", 1L))
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

        when(solicitudService.cambiarEstado(eq(1L), any(CambiarEstadoRequest.class), anyString()))
                .thenReturn(detalle);

        mockMvc.perform(patch("/solicitudes/{id}/estado", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("CITADO"));
    }
}