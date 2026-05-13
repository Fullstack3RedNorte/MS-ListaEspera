package cl.rednorte.ms_lista_espera.model.entity;

import cl.rednorte.ms_lista_espera.enums.EstadoSolicitud;
import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "historial_estados")
public class HistorialEstado {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "solicitud_id", nullable = false)
    private Solicitud solicitud;

    @Enumerated(EnumType.STRING)
    @Column
    private EstadoSolicitud estadoAnterior;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoSolicitud estadoNuevo;

    @Column
    private String motivo;

    @Column(nullable = false)
    private LocalDateTime fechaCambio;

    @Column(nullable = false)
    private String rutUsuarioResponsable;

    @PrePersist
    protected void onCreate() {
        fechaCambio = LocalDateTime.now();
    }
}