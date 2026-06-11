package cl.rednorte.ms_lista_espera.model.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.enums.NivelUrgencia;

@Data
@Entity
@Table(name = "solicitudes")
public class Solicitud {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String rutPaciente;

    @Column(nullable = false)
    private String rutFuncionario;

    @ManyToOne
    @JoinColumn(name = "especialidad_id", nullable = false)
    private Especialidad especialidad;

    @Column(nullable = false)
    private String diagnostico;

    @Column(nullable = false)
    private Boolean esGES = false;

    @Column
    private String patologiaGES;

    @Enumerated(EnumType.STRING)//guarda el texto del enum en la base de datos en lugar de un numero
    @Column(nullable = false)
    private NivelUrgencia nivelUrgencia;

    @Column(nullable = false)
    private Boolean esVulnerable = false;

    @ManyToOne
    @JoinColumn(name = "tipo_vulnerabilidad_id")
    private TipoVulnerabilidad tipoVulnerabilidad;

    @Column(nullable = false)
    private Integer prioridad;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estado = EstadoSolicitud.EN_ESPERA;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @Column(nullable = false)
    private LocalDateTime fechaActualizacion;

    // Fecha y hora asignada cuando la solicitud pasa a estado CITADO.
    // Nullable: solo tiene valor cuando la solicitud ha sido citada al menos una vez.
    @Column
    private LocalDateTime fechaCita;

    @PrePersist//se ejecuta antes de que la entidad se guarde por primera vez en la base de datos y asigna las fechas
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate //se ejecuta automaticamente antes de que la entidad se actualice en la base de datos y actualiza la fecha de actualización
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}