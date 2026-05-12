package cl.rednorte.ms_lista_espera.model.entity;

import java.time.LocalDateTime;
import java.util.List;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import cl.rednorte.ms_lista_espera.enums.NivelUrgencia;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;

@Entity
@Table(name = "solicitudes")
public class Solicitud {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private String rutPaciente;
    @Column (nullable = false)
    private String rutFuncionario;
    @Column (nullable = false)
    private String diagnostico;
    @Column (nullable = false)
    private Boolean esGES;
    @Column (nullable = false)
    private String patologiaGES;
    @Column (nullable = false)
    private Boolean esVulnerable;
    @Column (nullable = false)
    private Integer prioridad;

    @Enumerated(EnumType.STRING)
    private NivelUrgencia nivelUrgencia;

    @Enumerated(EnumType.STRING)
    private EstadoSolicitud estado;

    @ManyToOne
    @JoinColumn(name = "especialidad_id")
    private Especialidad especialidad;

    @ManyToOne
    @JoinColumn(name = "tipo_vulnerabilidad_id")
    private TipoVulnerabilidad tipoVulnerabilidad;

    private LocalDateTime fechaRegistro;
    private LocalDateTime fechaActualizacion;

    // Relación con el historial (1 a N)
    @OneToMany(mappedBy = "solicitud", cascade = CascadeType.ALL)
    private List<HistorialEstado> historial;

    @PrePersist
    protected void onCreate() {
        fechaRegistro = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }
}
