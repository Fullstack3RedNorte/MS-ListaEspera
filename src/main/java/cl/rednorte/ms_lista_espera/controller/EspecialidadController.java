package cl.rednorte.ms_lista_espera.controller;

import cl.rednorte.ms_lista_espera.dto.response.EspecialidadResponse;
import cl.rednorte.ms_lista_espera.service.EspecialidadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/especialidades")
@RequiredArgsConstructor
public class EspecialidadController {

    private final EspecialidadService especialidadService;

    @GetMapping
    public ResponseEntity<List<EspecialidadResponse>> listarActivas() {
        return ResponseEntity.ok(especialidadService.listarActivas());
    }
}