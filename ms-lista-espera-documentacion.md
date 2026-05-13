# MS Lista de Espera — Documentación Técnica
**RedNorte | Fullstack III | DuocUC**

---

## Índice

1. [Descripción general](#1-descripción-general)
2. [Arquetipo del microservicio](#2-arquetipo-del-microservicio)
3. [Arquitectura](#3-arquitectura)
4. [Patrones de diseño de software](#4-patrones-de-diseño-de-software)
5. [Stack tecnológico](#5-stack-tecnológico)
6. [Modelo de datos](#6-modelo-de-datos)
7. [Endpoints REST](#7-endpoints-rest)
8. [Configuración](#8-configuración)

---

## 1. Descripción general

El **MS Lista de Espera** es el microservicio central de la plataforma RedNorte. Es responsable de registrar y gestionar todas las solicitudes de atención médica que ingresan a la lista de espera hospitalaria.

El microservicio toma el proceso desde el momento en que una interconsulta es **aceptada** por el médico especialista en el sistema externo de RedNorte. A partir de ese punto, gestiona el ciclo de vida completo de la solicitud hasta que alcanza un estado terminal.

### Responsabilidades principales

- Registrar solicitudes de atención médica realizadas por funcionarios
- Calcular automáticamente la prioridad clínica según criterios GES, riesgo vital, vulnerabilidad y tiempo en lista
- Gestionar los estados del ciclo de vida de cada solicitud
- Registrar historial completo de cambios de estado para auditoría
- Exponer endpoints REST para consulta y gestión de solicitudes

---

## 2. Arquetipo del microservicio

### Estructura de carpetas

```
ms-lista-espera/
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── cl/rednorte/ms_lista_espera/
│   │   │       ├── controller/         # Capa de presentación — endpoints REST
│   │   │       ├── service/            # Interfaces de servicio
│   │   │       │   └── impl/           # Implementaciones de servicio
│   │   │       ├── repository/         # Interfaces JPA — acceso a datos
│   │   │       ├── model/
│   │   │       │   └── entity/         # Entidades JPA — mapeo a tablas MySQL
│   │   │       ├── dto/
│   │   │       │   ├── request/        # Objetos de entrada — datos que llegan
│   │   │       │   └── response/       # Objetos de salida — datos que retorna
│   │   │       ├── enums/              # Enumeraciones del dominio
│   │   │       ├── config/             # Configuraciones Spring
│   │   │       └── MsListaEsperaApplication.java
│   │   └── resources/
│   │       ├── application.yaml        # Configuración principal
│   │       └── application-dev.yaml   # Configuración local (no va a GitHub)
│   └── test/
│       └── java/cl/rednorte/ms_lista_espera/
│           ├── controller/
│           └── service/
├── .gitignore
├── pom.xml
└── README.md
```

### Convención de nombres

| Tipo | Convención | Ejemplo |
|------|-----------|---------|
| Entidades | PascalCase | `Solicitud`, `HistorialEstado` |
| Repositorios | Entidad + Repository | `SolicitudRepository` |
| Servicios (interfaz) | Entidad + Service | `SolicitudService` |
| Servicios (impl) | Entidad + ServiceImpl | `SolicitudServiceImpl` |
| Controladores | Entidad + Controller | `SolicitudController` |
| DTOs Request | Acción + Entidad + Request | `CrearSolicitudRequest` |
| DTOs Response | Entidad + Response | `SolicitudResponse` |
| Configuraciones | Tecnología + Config | `SecurityConfig`, `RabbitMQConfig` |

---

## 3. Arquitectura

### Posición en la arquitectura global

```
React SPA
    │
    ▼
BFF Gateway (puerto 8080)
    │ JWT validado por JWKS stateless
    │ Circuit Breaker — Resilience4j
    ▼
MS Lista de Espera (puerto 8081)
    │
    ├── MySQL — db_lista_espera
    └── RabbitMQ — notificaciones.queue / horas.queue
```

### Flujo interno de una petición

```
Request HTTP
    │
    ▼
Controller          # Recibe la petición, valida con @Valid, llama al Service
    │
    ▼
Service (impl)      # Lógica de negocio: calcularPrioridad, validarTransicion
    │
    ▼
Repository          # Interfaz JPA — consulta o persiste en MySQL
    │
    ▼
MySQL               # Base de datos db_lista_espera
    │
    ▼
Response DTO        # El Service construye el DTO y lo retorna al Controller
    │
    ▼
Response HTTP       # El Controller retorna el ResponseEntity con código HTTP
```

### Comunicación con otros microservicios

| Dirección | Destino | Mecanismo | Propósito |
|-----------|---------|-----------|-----------|
| Entrada | BFF Gateway | REST/HTTP | Recibe peticiones de funcionarios y pacientes |
| Salida | MS Notificaciones | RabbitMQ — notificaciones.queue | Publica eventos de cambio de estado |
| Salida | RRHH | RabbitMQ — horas.queue | Informa horas asignadas o liberadas |

---

## 4. Patrones de diseño de software

### 4.1. Repository Pattern

**¿Qué es?**
Crea una capa de abstracción entre la lógica de negocio y el acceso a datos. El código de negocio nunca habla directamente con la base de datos — siempre lo hace a través del repositorio.

**¿Dónde se aplica?**
En todas las interfaces de la carpeta `repository/`:
- `SolicitudRepository`
- `EspecialidadRepository`
- `TipoVulnerabilidadRepository`
- `HistorialEstadoRepository`

**¿Cómo se implementa?**
```java
@Repository
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {
    Page<Solicitud> findByFiltros(
        Long especialidadId,
        EstadoSolicitud estado,
        String rutPaciente,
        Pageable pageable
    );
}
```

**¿Por qué se usa?**
- La lógica de negocio no depende de MySQL — si se cambia el motor de BD, solo se cambia la configuración, no el código de negocio
- Facilita las pruebas unitarias — se puede crear un repositorio falso (mock) sin necesitar la BD real
- Spring Data JPA genera automáticamente las queries básicas (CRUD) sin necesidad de escribir SQL manual

---

### 4.2. DTO Pattern (Data Transfer Object)

**¿Qué es?**
Separa los objetos que viajan entre capas de las entidades de base de datos. Define exactamente qué datos entran y qué datos salen del sistema.

**¿Dónde se aplica?**
En las carpetas `dto/request/` y `dto/response/`:

| DTO | Dirección | Propósito |
|-----|-----------|-----------|
| `CrearSolicitudRequest` | Entrada | Datos del formulario de registro |
| `CambiarEstadoRequest` | Entrada | Nuevo estado y motivo |
| `SolicitudResponse` | Salida | Vista resumida para listas |
| `SolicitudDetalleResponse` | Salida | Vista completa con historial |
| `PageResponse<T>` | Salida | Wrapper genérico para listas paginadas |

**¿Cómo se implementa?**
```java
@Data
public class CrearSolicitudRequest {
    @NotBlank(message = "El RUT del paciente es obligatorio")
    private String rutPaciente;

    @NotNull(message = "La especialidad es obligatoria")
    private Long especialidadId;
    // ...
}
```

**¿Por qué se usa?**
- Controla exactamente qué información se expone al frontend — evita exponer datos internos de la BD
- Las validaciones con `@NotBlank` y `@NotNull` se ejecutan automáticamente antes de llegar al Service
- Desacopla la API REST del modelo de datos interno — se puede cambiar la estructura de la BD sin cambiar los endpoints

---

### 4.3. Service Layer Pattern

**¿Qué es?**
Separa la lógica de negocio en una capa dedicada entre el Controller y el Repository. El Controller solo recibe y responde peticiones — nunca contiene lógica de negocio.

**¿Dónde se aplica?**
En la carpeta `service/` e `service/impl/`:
- `SolicitudService` — interfaz que define el contrato
- `SolicitudServiceImpl` — implementación con la lógica real
- `EspecialidadService` — interfaz
- `EspecialidadServiceImpl` — implementación

**¿Cómo se implementa?**
```java
// Interfaz — define el contrato
public interface SolicitudService {
    SolicitudResponse crear(CrearSolicitudRequest request, String rutFuncionario);
    PageResponse<SolicitudResponse> listar(...);
    SolicitudDetalleResponse obtenerDetalle(Long id);
    SolicitudDetalleResponse cambiarEstado(Long id, CambiarEstadoRequest request, String rutFuncionario);
}

// Implementación — contiene la lógica real
@Service
@RequiredArgsConstructor
public class SolicitudServiceImpl implements SolicitudService {
    @Override
    @Transactional
    public SolicitudResponse crear(CrearSolicitudRequest request, String rutFuncionario) {
        // lógica de negocio aquí
    }
}
```

**¿Por qué se usa?**
- Separa responsabilidades — el Controller sabe cómo comunicarse HTTP, el Service sabe cómo resolver el negocio
- La interfaz permite cambiar la implementación sin afectar al Controller
- `@Transactional` garantiza que si algo falla en medio de una operación, todo se revierte automáticamente

---

### 4.4. Enum Pattern

**¿Qué es?**
Define conjuntos fijos de valores válidos para atributos del dominio, evitando valores arbitrarios o inconsistentes.

**¿Dónde se aplica?**
En la carpeta `enums/`:
- `EstadoSolicitud` — estados del ciclo de vida
- `NivelUrgencia` — nivel de prioridad clínica

**¿Cómo se implementa?**
```java
public enum EstadoSolicitud {
    EN_ESPERA, CITADO, ATENDIDO, AUSENTE,
    CERRADO, ANULADO, DERIVADO, VENCIDO
}

// En la entidad
@Enumerated(EnumType.STRING)
@Column(nullable = false)
private EstadoSolicitud estado = EstadoSolicitud.EN_ESPERA;
```

**¿Por qué se usa?**
- Evita valores inválidos — MySQL rechaza cualquier valor que no esté en el enum
- `EnumType.STRING` guarda el texto legible en la BD en lugar de un número — facilita la auditoría
- El compilador detecta errores en tiempo de compilación si se usa un valor inexistente

---

### 4.5. Token Propagation Pattern

**¿Qué es?**
Los datos del usuario autenticado viajan dentro del JWT. Los microservicios extraen esa información directamente del token sin consultar al IdP en cada petición.

**¿Dónde se aplica?**
En `SolicitudController.java`:

```java
@PostMapping
public ResponseEntity<SolicitudResponse> crear(
        @Valid @RequestBody CrearSolicitudRequest request,
        @AuthenticationPrincipal Jwt jwt) {

    String rutFuncionario = (jwt != null)
        ? jwt.getClaimAsString("rut")
        : "11111111-1"; // valor por defecto para pruebas

    return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(solicitudService.crear(request, rutFuncionario));
}
```

**¿Por qué se usa?**
- Elimina dependencias síncronas con el IdP externo — si el IdP falla, los usuarios ya autenticados siguen operando
- El RUT del funcionario se extrae del token sin hacer una consulta adicional a ningún servicio externo
- Reduce la latencia — no hay llamadas extra por cada petición

---

### 4.6. State Pattern (implícito en validarTransicion)

**¿Qué es?**
Controla las transiciones válidas entre estados de un objeto. Solo permite cambios de estado que sean coherentes con el ciclo de vida definido.

**¿Dónde se aplica?**
En `SolicitudServiceImpl.java`, método `esTransicionValida()`:

```java
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
```

**¿Por qué se usa?**
- Garantiza la integridad del ciclo de vida — un paciente no puede pasar de EN_ESPERA a ATENDIDO sin pasar por CITADO
- Centraliza las reglas de transición en un solo lugar — fácil de mantener y auditar
- Usa Switch Expression de Java 21 — más limpio y expresivo que if/else anidados

---

### 4.7. Audit Pattern (con @PrePersist y @PreUpdate)

**¿Qué es?**
Registra automáticamente cuándo se creó y modificó un registro, sin necesidad de que el código de negocio lo gestione explícitamente.

**¿Dónde se aplica?**
En las entidades `Solicitud.java` e `HistorialEstado.java`:

```java
@PrePersist
protected void onCreate() {
    fechaRegistro = LocalDateTime.now();
    fechaActualizacion = LocalDateTime.now();
}

@PreUpdate
protected void onUpdate() {
    fechaActualizacion = LocalDateTime.now();
}
```

**¿Por qué se usa?**
- Las fechas se asignan automáticamente — el Service no necesita preocuparse por ello
- Garantiza que ningún registro quede sin fecha de creación o modificación
- Facilita la auditoría — siempre se sabe cuándo ocurrió cada cambio

---

## 5. Stack tecnológico

| Tecnología | Versión | Propósito |
|-----------|---------|-----------|
| Java | 21 LTS | Lenguaje de programación |
| Spring Boot | 3.5.14 | Framework principal |
| Spring Data JPA | 3.5.14 | Persistencia y acceso a datos |
| Spring Security | 6.5.10 | Autenticación y autorización JWT |
| Spring AMQP | 3.5.14 | Mensajería con RabbitMQ |
| Hibernate | 6.6.49 | ORM — mapeo objeto-relacional |
| MySQL | 8.0 | Base de datos relacional |
| HikariCP | 6.3.3 | Pool de conexiones a BD |
| Lombok | latest | Reducción de código boilerplate |
| Maven | 3.9.15 | Gestión de dependencias y build |

---

## 6. Modelo de datos

### Diagrama de tablas

```
especialidades
├── id (PK, BIGINT, AUTO_INCREMENT)
├── nombre (VARCHAR, NOT NULL, UNIQUE)
├── descripcion (VARCHAR)
└── activo (BIT, NOT NULL)

tipos_vulnerabilidad
├── id (PK, BIGINT, AUTO_INCREMENT)
├── nombre (VARCHAR, NOT NULL, UNIQUE)
└── descripcion (VARCHAR)

solicitudes
├── id (PK, BIGINT, AUTO_INCREMENT)
├── rut_paciente (VARCHAR, NOT NULL)
├── rut_funcionario (VARCHAR, NOT NULL)
├── especialidad_id (FK → especialidades.id, NOT NULL)
├── diagnostico (VARCHAR, NOT NULL)
├── esges (BIT, NOT NULL)
├── patologiages (VARCHAR)
├── nivel_urgencia (ENUM, NOT NULL)
├── es_vulnerable (BIT, NOT NULL)
├── tipo_vulnerabilidad_id (FK → tipos_vulnerabilidad.id)
├── prioridad (INT, NOT NULL)
├── estado (ENUM, NOT NULL)
├── fecha_registro (DATETIME, NOT NULL)
└── fecha_actualizacion (DATETIME, NOT NULL)

historial_estados
├── id (PK, BIGINT, AUTO_INCREMENT)
├── solicitud_id (FK → solicitudes.id, NOT NULL)
├── estado_anterior (ENUM)
├── estado_nuevo (ENUM, NOT NULL)
├── motivo (VARCHAR)
├── fecha_cambio (DATETIME, NOT NULL)
└── rut_usuario_responsable (VARCHAR, NOT NULL)
```

### Ciclo de vida de una solicitud

```
[Interconsulta aceptada]
         │
         ▼
     EN_ESPERA ──────────────────► ANULADO (terminal)
         │                          │
         │                          ▼
         ▼                        VENCIDO (terminal)
      CITADO ──────────────────► ANULADO (terminal)
         │                       VENCIDO (terminal)
         │                       DERIVADO (terminal)
         ▼
     ATENDIDO ──────────────────► DERIVADO (terminal)
         │
         ▼
      CERRADO (terminal)

     AUSENTE (1ra vez) ──────────► EN_ESPERA
     AUSENTE (2da vez) ──────────► CERRADO (terminal)
```

### Lógica de prioridad

| Prioridad | Condición |
|-----------|-----------|
| 1 (más alta) | esGES = true o nivelUrgencia = GES |
| 2 | nivelUrgencia = URGENTE |
| 3 | esVulnerable = true |
| 4 (más baja) | nivelUrgencia = ELECTIVA |

---

## 7. Endpoints REST

### Base URL
```
http://localhost:8081
```

### GET /especialidades
Retorna la lista de especialidades activas.

**Request:**
```
GET /especialidades
```

**Response 200:**
```json
[
  {
    "id": 1,
    "nombre": "Cardiología",
    "descripcion": "Especialidad del corazón"
  }
]
```

---

### POST /solicitudes
Registra una nueva solicitud en lista de espera.

**Request:**
```json
{
  "rutPaciente": "12345678-9",
  "especialidadId": 1,
  "diagnostico": "Dolor torácico crónico",
  "esGES": true,
  "patologiaGES": "Infarto agudo al miocardio",
  "nivelUrgencia": "GES",
  "esVulnerable": true,
  "tipoVulnerabilidadId": 1
}
```

**Response 201:**
```json
{
  "id": 1,
  "rutPaciente": "12345678-9",
  "especialidad": "Cardiología",
  "prioridad": 1,
  "estado": "EN_ESPERA",
  "fechaRegistro": "2026-05-13T10:00:00"
}
```

---

### GET /solicitudes
Retorna lista paginada con filtros opcionales.

**Query params:**
| Parámetro | Tipo | Requerido | Descripción |
|-----------|------|-----------|-------------|
| especialidadId | Long | No | Filtrar por especialidad |
| estado | EstadoSolicitud | No | Filtrar por estado |
| rutPaciente | String | No | Filtrar por RUT |
| page | int | No (default 0) | Número de página |
| size | int | No (default 20) | Tamaño de página |
| ordenarPor | String | No (default prioridad) | Campo de ordenamiento |

**Response 200:**
```json
{
  "content": [...],
  "totalElements": 83,
  "totalPages": 5,
  "currentPage": 0
}
```

---

### GET /solicitudes/{id}
Retorna detalle completo de una solicitud con historial.

**Response 200:**
```json
{
  "id": 1,
  "rutPaciente": "12345678-9",
  "rutFuncionario": "11111111-1",
  "especialidad": "Cardiología",
  "diagnostico": "Dolor torácico crónico",
  "esGES": true,
  "patologiaGES": "Infarto agudo al miocardio",
  "nivelUrgencia": "GES",
  "esVulnerable": true,
  "tipoVulnerabilidad": "Adulto mayor",
  "prioridad": 1,
  "estado": "EN_ESPERA",
  "fechaRegistro": "2026-05-13T10:00:00",
  "fechaActualizacion": "2026-05-13T10:00:00",
  "historial": [
    {
      "estadoAnterior": null,
      "estadoNuevo": "EN_ESPERA",
      "motivo": null,
      "fechaCambio": "2026-05-13T10:00:00",
      "rutUsuarioResponsable": "11111111-1"
    }
  ]
}
```

**Response 404:** Solicitud no encontrada

---

### PATCH /solicitudes/{id}/estado
Cambia el estado de una solicitud.

**Request:**
```json
{
  "nuevoEstado": "CITADO",
  "motivo": null
}
```

**Response 200:** SolicitudDetalleResponse actualizado

**Response 400:** Transición no válida

**Response 422:** Motivo obligatorio faltante (para ANULADO y DERIVADO)

**Response 404:** Solicitud no encontrada

---

## 8. Configuración

### application.yaml

```yaml
spring:
  application:
    name: ms-lista-espera
  datasource:
    url: jdbc:mysql://localhost:3306/db_lista_espera
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:}
    driver-class-name: com.mysql.cj.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: update
    show-sql: true
    open-in-view: false
    properties:
      hibernate:
        format_sql: true
  rabbitmq:
    host: ${RABBITMQ_HOST:localhost}
    port: ${RABBITMQ_PORT:5672}
    username: ${RABBITMQ_USERNAME:guest}
    password: ${RABBITMQ_PASSWORD:guest}
  security:
    oauth2:
      resourceserver:
        jwt:
          jwk-set-uri: ${IDP_JWKS_URI:http://localhost:9000/.well-known/jwks.json}
server:
  port: 8081
```

### Variables de entorno

| Variable | Descripción | Default |
|----------|-------------|---------|
| DB_USERNAME | Usuario MySQL | root |
| DB_PASSWORD | Contraseña MySQL | (vacío) |
| RABBITMQ_HOST | Host de RabbitMQ | localhost |
| RABBITMQ_PORT | Puerto de RabbitMQ | 5672 |
| RABBITMQ_USERNAME | Usuario RabbitMQ | guest |
| RABBITMQ_PASSWORD | Contraseña RabbitMQ | guest |
| IDP_JWKS_URI | URL del proveedor de identidad | localhost:9000 |

### Cómo ejecutar localmente

```bash
# 1. Clonar el repositorio
git clone https://github.com/Fullstack3RedNorte/MS-ListaEspera.git

# 2. Iniciar XAMPP (MySQL) y Docker (RabbitMQ)
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management

# 3. Crear la base de datos en phpMyAdmin
# http://localhost/phpmyadmin → crear db_lista_espera

# 4. Ejecutar el proyecto
cd MS-ListaEspera
mvn spring-boot:run


```

Docker crea un contenedor — piénsalo como una caja aislada que tiene todo lo que necesita adentro. Cuando ejecutaste el comando, Docker descargó una imagen que ya incluye:
Erlang    ← ya viene instalado adentro del contenedor
RabbitMQ  ← ya viene instalado adentro del contenedor
Todo vive dentro de esa caja aislada, completamente independiente de tu Windows. No importa qué versión de Erlang tengas instalada en tu PC — Docker usa la suya propia.
docker run -d --name rabbitmq -p 5672:5672 -p 15672:15672 rabbitmq:3.13-management
Qué hace este comando:
-d              → corre en segundo plano
--name rabbitmq → nombre del contenedor
-p 5672:5672    → puerto de mensajería
-p 15672:15672  → puerto del panel de administración
rabbitmq:3.13-management → imagen con panel incluido

Espera que descargue la imagen y luego verifica en el navegador:
http://localhost:15672
Usuario: guest
Password: guest
---

*Documentación generada para el proyecto semestral Fullstack III — RedNorte — DuocUC 2026*
