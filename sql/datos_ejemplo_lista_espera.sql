USE db_lista_espera;

SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE historial_estados;
TRUNCATE TABLE solicitudes;
TRUNCATE TABLE tipos_vulnerabilidad;
TRUNCATE TABLE especialidades;
SET FOREIGN_KEY_CHECKS = 1;

INSERT INTO especialidades (nombre, descripcion, activo) VALUES
('Cardiología', 'Atención de patologías cardiovasculares', 1),
('Neurología', 'Atención de patologías neurológicas', 1),
('Traumatología', 'Atención de lesiones osteomusculares', 1),
('Oftalmología', 'Atención de salud visual', 1);

INSERT INTO tipos_vulnerabilidad (nombre, descripcion) VALUES
('Adulto Mayor', 'Paciente de 60 años o más'),
('Discapacidad', 'Paciente en situación de discapacidad'),
('Riesgo Social', 'Paciente con condición social de riesgo');

INSERT INTO solicitudes (
  rut_paciente,
  rut_funcionario,
  especialidad_id,
  diagnostico,
  esges,
  patologiages,
  nivel_urgencia,
  prioridad,
  es_vulnerable,
  tipo_vulnerabilidad_id,
  estado,
  fecha_registro,
  fecha_actualizacion
) VALUES
(
  '11111111-1',
  '16543210-9',
  (SELECT id FROM especialidades WHERE nombre = 'Cardiología'),
  'Hipertensión arterial de difícil manejo',
  1,
  'HTA GES',
  'GES',
  1,
  1,
  (SELECT id FROM tipos_vulnerabilidad WHERE nombre = 'Adulto Mayor'),
  'EN_ESPERA',
  NOW(),
  NOW()
),
(
  '22222222-2',
  '17456321-0',
  (SELECT id FROM especialidades WHERE nombre = 'Neurología'),
  'Cefalea crónica con signos de alarma',
  0,
  NULL,
  'URGENTE',
  2,
  0,
  NULL,
  'CITADO',
  NOW(),
  NOW()
),
(
  '33333333-3',
  '18345678-5',
  (SELECT id FROM especialidades WHERE nombre = 'Traumatología'),
  'Dolor lumbar crónico con limitación funcional',
  0,
  NULL,
  'ELECTIVA',
  3,
  1,
  (SELECT id FROM tipos_vulnerabilidad WHERE nombre = 'Riesgo Social'),
  'EN_ESPERA',
  NOW(),
  NOW()
),
(
  '44444444-4',
  '19234567-4',
  (SELECT id FROM especialidades WHERE nombre = 'Oftalmología'),
  'Disminución progresiva de agudeza visual',
  1,
  'Cataratas GES',
  'GES',
  1,
  1,
  (SELECT id FROM tipos_vulnerabilidad WHERE nombre = 'Discapacidad'),
  'ATENDIDO',
  NOW(),
  NOW()
);

INSERT INTO historial_estados (
  solicitud_id,
  estado_anterior,
  estado_nuevo,
  motivo,
  rut_usuario_responsable,
  fecha_cambio
) VALUES
(
  (SELECT id FROM solicitudes WHERE rut_paciente = '11111111-1' LIMIT 1),
  NULL,
  'EN_ESPERA',
  'Ingreso inicial de solicitud',
  '16543210-9',
  NOW()
),
(
  (SELECT id FROM solicitudes WHERE rut_paciente = '22222222-2' LIMIT 1),
  'EN_ESPERA',
  'CITADO',
  'Se asigna primera hora de atención',
  '17456321-0',
  NOW()
),
(
  (SELECT id FROM solicitudes WHERE rut_paciente = '44444444-4' LIMIT 1),
  'CITADO',
  'ATENDIDO',
  'Paciente atendido según agenda',
  '19234567-4',
  NOW()
);

SELECT 'especialidades' AS tabla, COUNT(*) AS total FROM especialidades
UNION ALL
SELECT 'tipos_vulnerabilidad' AS tabla, COUNT(*) AS total FROM tipos_vulnerabilidad
UNION ALL
SELECT 'solicitudes' AS tabla, COUNT(*) AS total FROM solicitudes
UNION ALL
SELECT 'historial_estados' AS tabla, COUNT(*) AS total FROM historial_estados;
