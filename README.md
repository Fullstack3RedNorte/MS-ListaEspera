# Pruebas Unitarias — MS-ListaEspera

## Descripción General

Este documento describe las pruebas unitarias implementadas para el microservicio **MS-ListaEspera**, perteneciente al sistema RedNorte. Las pruebas están organizadas en dos clases principales que cubren la capa de servicio y la capa de controlador, usando **JUnit 5**, **Mockito** y **MockMvc** sin necesidad de levantar contexto Spring ni base de datos.

***

## Tecnologías Utilizadas

| Herramienta | Versión | Propósito |
|---|---|---|
| JUnit 5 | Incluida en Spring Boot 3.x | Framework de pruebas |
| Mockito | Incluida en Spring Boot 3.x | Mocking de dependencias |
| AssertJ | Incluida en Spring Boot 3.x | Assertions fluidas |
| MockMvc (standalone) | Incluida en Spring Boot 3.x | Pruebas de controlador sin contexto |

***

## Estructura de Archivos

```
src/test/java/cl/rednorte/ms_lista_espera/
├── MsListaEsperaApplicationTests.java         ← Test de contexto (deshabilitado, requiere MySQL/RabbitMQ)
├── controller/
│   └── SolicitudControllerTest.java           ← Pruebas de la capa controlador
└── service/
    └── impl/
        └── SolicitudServiceImplTest.java      ← Pruebas de la capa de servicio
```

***

## Clase 1: SolicitudServiceImplTest

Prueba la lógica de negocio de `SolicitudServiceImpl` aislando todas las dependencias con mocks.

**Configuración:** `@ExtendWith(MockitoExtension.class)` — no levanta contexto Spring.

**Dependencias mockeadas:** `SolicitudRepository`, `EspecialidadRepository`, `TipoVulnerabilidadRepository`, `HistorialEstadoRepository`.

### Casos de prueba

| Método del test | Método testeado | Escenario | Resultado esperado |
|---|---|---|---|
| `crear_solicitudNormal_debeRetornarResponse` | `crear()` | Request válido con datos normales | Retorna `SolicitudResponse` con estado `EN_ESPERA` |
| `crear_especialidadNoExiste_debeArrojarNotFound` | `crear()` | `especialidadId` inexistente | Lanza `ResponseStatusException` 404 |
| `crear_esGES_debeTenerPrioridadUno` | `crear()` | Solicitud con `esGES = true` | Retorna prioridad = 1 |
| `obtenerDetalle_idExistente_debeRetornarDetalle` | `obtenerDetalle()` | ID válido existente | Retorna `SolicitudDetalleResponse` con datos correctos |
| `obtenerDetalle_idNoExistente_debeArrojarNotFound` | `obtenerDetalle()` | ID inexistente | Lanza `ResponseStatusException` 404 |
| `cambiarEstado_transicionValidaEnEsperaACitado_debeActualizar` | `cambiarEstado()` | Transición válida `EN_ESPERA → CITADO` con fecha futura | Retorna estado `CITADO` |
| `cambiarEstado_transicionInvalida_debeArrojarBadRequest` | `cambiarEstado()` | Transición inválida desde `CERRADO` | Lanza `ResponseStatusException` 400 |
| `cambiarEstado_anuladoSinMotivo_debeArrojarUnprocessable` | `cambiarEstado()` | Estado `ANULADO` sin motivo | Lanza `ResponseStatusException` 422 |
| `cambiarEstado_citadoConFechaPasada_debeArrojarUnprocessable` | `cambiarEstado()` | `fechaCita` en el pasado | Lanza `ResponseStatusException` 422 |

***

## Clase 2: SolicitudControllerTest

Prueba los endpoints REST de `SolicitudController` usando MockMvc en modo standalone (sin contexto Spring ni seguridad activa).

**Configuración:** `@ExtendWith(MockitoExtension.class)` + `MockMvcBuilders.standaloneSetup()`.

**Dependencias mockeadas:** `SolicitudService`.

### Casos de prueba

| Método del test | Endpoint | Escenario | Resultado esperado |
|---|---|---|---|
| `crear_requestValido_debeRetornar201` | `POST /solicitudes` | Request válido con todos los campos | HTTP 201, body con estado `EN_ESPERA` |
| `obtenerDetalle_idExistente_debeRetornar200` | `GET /solicitudes/{id}` | ID existente | HTTP 200, body con `rutPaciente` correcto |
| `cambiarEstado_requestValido_debeRetornar200` | `PATCH /solicitudes/{id}/estado` | Cambio de estado válido a `CITADO` | HTTP 200, body con estado `CITADO` |

***

## Lógica de Negocio Cubierta

Las pruebas verifican las siguientes reglas críticas del sistema:

- **Cálculo de prioridad:** Las solicitudes GES reciben prioridad 1 (máxima urgencia).
- **Validación de especialidad:** No se puede crear una solicitud con una especialidad inexistente.
- **Transiciones de estado:** Solo se permiten las transiciones definidas en `esTransicionValida()`.
- **Motivo obligatorio:** Los estados `ANULADO` y `DERIVADO` requieren motivo.
- **Fecha de cita futura:** Al citar un paciente, la `fechaCita` debe ser posterior al momento actual.

***

## Cómo Ejecutar las Pruebas

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar solo el test de servicio
./mvnw test -Dtest=SolicitudServiceImplTest

# Ejecutar solo el test de controlador
./mvnw test -Dtest=SolicitudControllerTest
```

### Resultado esperado

```
Tests run: 10, Failures: 0, Errors: 0, Skipped: 0
BUILD SUCCESS
```

> **Nota:** `MsListaEsperaApplicationTests` está deshabilitado intencionalmente, ya que el test de contexto completo requiere conexión activa a MySQL y RabbitMQ.