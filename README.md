# Pruebas Unitarias — MS-ListaEspera

Documentación de las pruebas unitarias implementadas para el microservicio **MS-ListaEspera** en la rama `pruebas-unitarias`.

## Cobertura JaCoCo

![Reporte JaCoCo](docs/jacoco-reporte.png)

| Paquete | Instrucciones | Ramas |
|---|---:|---:|
| `cl.rednorte.ms_lista_espera.service.impl` | 72% | 54% |
| `cl.rednorte.ms_lista_espera.controller` | 92% | 100% |
| `cl.rednorte.ms_lista_espera.model.entity` | 0% | n/a |
| `cl.rednorte.ms_lista_espera` | 37% | n/a |
| `cl.rednorte.ms_lista_espera.enums` | 100% | n/a |
| **Total** | **71%** | **58%** |

> La cobertura total incluye paquetes de soporte como entidades y clases base del proyecto, por eso el porcentaje general no refleja solo la lógica testeada. La revisión principal debe centrarse en `service.impl` y `controller`.

---

## Objetivo

El objetivo de estas pruebas es validar la lógica de negocio del microservicio y verificar el comportamiento de los endpoints principales de `SolicitudController`, asegurando que las operaciones críticas respondan correctamente ante escenarios exitosos y errores esperados.

---

## Tecnologías utilizadas

- **JUnit 5** para la ejecución de pruebas unitarias.
- **Mockito** para la simulación de dependencias.
- **Spring MockMvc** para probar endpoints HTTP sin levantar el servidor completo.
- **JaCoCo** para medir cobertura de código.
- **Maven Surefire** para la ejecución de los tests.

---

## Estructura de pruebas

```text
src/test/java/cl/rednorte/ms_lista_espera/
├── controller/
│   └── SolicitudControllerTest.java
└── service/
    └── impl/
        └── SolicitudServiceImplTest.java
```

---

## Pruebas del servicio

Archivo: `SolicitudServiceImplTest.java`

Estas pruebas validan la lógica interna de `SolicitudServiceImpl` usando mocks de los repositorios, sin necesidad de levantar el contexto completo de Spring.

### Métodos cubiertos

#### `crear()`

| Test | Escenario | Resultado esperado |
|---|---|---|
| `crear_solicitudNormal_debeRetornarResponse` | Solicitud válida con especialidad existente | Retorna un `SolicitudResponse` con estado `EN_ESPERA` |
| `crear_especialidadNoExiste_debeArrojarNotFound` | Se intenta crear con una especialidad inexistente | Lanza `ResponseStatusException` con estado 404 |
| `crear_esGES_debeTenerPrioridadUno` | La solicitud se marca como GES | La prioridad calculada es `1` |

#### `obtenerDetalle()`

| Test | Escenario | Resultado esperado |
|---|---|---|
| `obtenerDetalle_idExistente_debeRetornarDetalle` | El ID existe en base de datos | Retorna el detalle completo de la solicitud |
| `obtenerDetalle_idNoExistente_debeArrojarNotFound` | El ID no existe | Lanza `ResponseStatusException` con estado 404 |

#### `cambiarEstado()`

| Test | Escenario | Resultado esperado |
|---|---|---|
| `cambiarEstado_transicionValidaEnEsperaACitado_debeActualizar` | Cambio válido de `EN_ESPERA` a `CITADO` con fecha futura | Retorna el detalle actualizado con estado `CITADO` |
| `cambiarEstado_transicionInvalida_debeArrojarBadRequest` | Cambio inválido desde `CERRADO` a `CITADO` | Lanza `ResponseStatusException` con estado 400 |
| `cambiarEstado_anuladoSinMotivo_debeArrojarUnprocessable` | Se intenta anular sin motivo | Lanza `ResponseStatusException` indicando que el motivo es obligatorio |
| `cambiarEstado_citadoConFechaPasada_debeArrojarUnprocessable` | Se intenta citar con fecha pasada | Lanza `ResponseStatusException` indicando que la cita debe ser futura |

---

## Pruebas del controller

Archivo: `SolicitudControllerTest.java`

Estas pruebas validan la capa HTTP de `SolicitudController` usando `@WebMvcTest`, `MockMvc` y una configuración de seguridad de test para permitir las peticiones y desactivar CSRF durante la ejecución.

### Endpoints cubiertos

| Test | Método | Endpoint | Resultado esperado |
|---|---|---|---|
| `crear_requestValido_debeRetornar201` | `POST` | `/solicitudes` | Retorna HTTP 201 y estado `EN_ESPERA` |
| `listar_sinFiltros_debeRetornar200` | `GET` | `/solicitudes` | Retorna HTTP 200 y una respuesta paginada |
| `obtenerDetalle_idExistente_debeRetornar200` | `GET` | `/solicitudes/{id}` | Retorna HTTP 200 con el `rutPaciente` esperado |
| `cambiarEstado_requestValido_debeRetornar200` | `PATCH` | `/solicitudes/{id}/estado` | Retorna HTTP 200 y estado `CITADO` |

---

## Consideraciones técnicas

- En `SolicitudController`, el parámetro `@AuthenticationPrincipal Jwt jwt` tiene un fallback interno al rut `11111111-1` cuando el token es `null`, por lo que los tests pueden ejecutarse sin depender de autenticación real.
- Para evitar errores `401` y `403` en `@WebMvcTest`, se utilizó una configuración de seguridad de prueba con `permitAll()` y CSRF deshabilitado.
- La cobertura del paquete `controller` mejoró al ejecutar los tests dentro del contexto Web MVC, permitiendo que JaCoCo registre correctamente las invocaciones al controlador.

---

## Cómo ejecutar las pruebas

```bash
./mvnw test
```

Este comando:

1. Compila el proyecto.
2. Ejecuta las pruebas unitarias.
3. Genera el reporte de cobertura JaCoCo.

---

## Cómo abrir el reporte JaCoCo

### En Windows

```bash
start target/site/jacoco/index.html
```

### En macOS o Linux

```bash
open target/site/jacoco/index.html
```

Ruta del reporte:

```text
target/site/jacoco/index.html
```

---

## Ejecutar una clase de prueba específica

```bash
./mvnw test -Dtest="cl.rednorte.ms_lista_espera.service.impl.SolicitudServiceImplTest"
./mvnw test -Dtest="cl.rednorte.ms_lista_espera.controller.SolicitudControllerTest"
```

---

## Captura del reporte

Para mostrar la evidencia visual de cobertura en GitHub, se agregó una captura del reporte JaCoCo en:

```text
docs/jacoco-reporte.png
```

La imagen se referencia en este documento con la siguiente línea:

```md

```

---

## Resultado final

Las pruebas implementadas cubren los flujos principales del servicio y del controlador para el microservicio **MS-ListaEspera**, entregando una base sólida de validación automática y una cobertura general del **71%**.