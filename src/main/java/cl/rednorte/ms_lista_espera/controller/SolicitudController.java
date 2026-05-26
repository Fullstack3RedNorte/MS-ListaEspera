package cl.rednorte.ms_lista_espera.controller;

import cl.rednorte.ms_lista_espera.dto.request.CambiarEstadoRequest;
import cl.rednorte.ms_lista_espera.dto.request.CrearSolicitudRequest;
import cl.rednorte.ms_lista_espera.dto.response.PageResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudDetalleResponse;
import cl.rednorte.ms_lista_espera.dto.response.SolicitudResponse;
import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.service.SolicitudService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/solicitudes")
@RequiredArgsConstructor
public class SolicitudController {

    private final SolicitudService solicitudService;

    @PostMapping
    public ResponseEntity<SolicitudResponse> crear(
            @Valid @RequestBody CrearSolicitudRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String rutFuncionario = (jwt != null)
            ? jwt.getClaimAsString("rut")
            : "11111111-1";

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(solicitudService.crear(request, rutFuncionario));
    }

    @GetMapping
    public ResponseEntity<PageResponse<SolicitudResponse>> listar(
            @RequestParam(required = false) Long especialidadId,
            @RequestParam(required = false) EstadoSolicitud estado,
            @RequestParam(required = false) String rutPaciente,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "prioridad") String ordenarPor) {

        Pageable pageable = PageRequest.of(page, size, Sort.by(ordenarPor).ascending());

        return ResponseEntity.ok(
            solicitudService.listar(especialidadId, estado, rutPaciente, pageable)
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<SolicitudDetalleResponse> obtenerDetalle(@PathVariable Long id) {
        return ResponseEntity.ok(solicitudService.obtenerDetalle(id));
    }

    @PatchMapping("/{id}/estado")
    public ResponseEntity<SolicitudDetalleResponse> cambiarEstado(
            @PathVariable Long id,
            @Valid @RequestBody CambiarEstadoRequest request,
            @AuthenticationPrincipal Jwt jwt) {

        String rutFuncionario = (jwt != null)
            ? jwt.getClaimAsString("rut")
            : "11111111-1";

        return ResponseEntity.ok(
            solicitudService.cambiarEstado(id, request, rutFuncionario)
        );
    }
}