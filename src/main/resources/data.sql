-- ===========================================================
-- data.sql — Datos iniciales del MS Lista de Espera
-- ===========================================================
-- Se ejecuta automáticamente al arrancar Spring Boot,
-- después de que Hibernate cree las tablas (gracias a
-- defer-datasource-initialization: true en application.yaml).
--
-- Todos los INSERT usan "WHERE NOT EXISTS" para que el archivo
-- sea idempotente: si la tabla ya tiene datos, no se duplican.
--
-- ⚠️ Para que las solicitudes y el historial se carguen bien,
-- la BD debe estar limpia al arrancar. Si ya tienes datos viejos,
-- ejecuta antes:  docker compose down -v
-- ===========================================================


-- ===========================================================
-- 1. ESPECIALIDADES (4 registros)
-- ===========================================================
INSERT INTO especialidades (nombre, descripcion, activo)
SELECT * FROM (
    SELECT 'Cardiología', 'Especialidad del corazón', true UNION ALL
    SELECT 'Broncopulmonar', 'Especialidad del sistema respiratorio', true UNION ALL
    SELECT 'Traumatología', 'Especialidad del sistema músculo esquelético', true UNION ALL
    SELECT 'Neurología', 'Especialidad del sistema nervioso', true
) AS nuevas
WHERE NOT EXISTS (SELECT 1 FROM especialidades);


-- ===========================================================
-- 2. TIPOS DE VULNERABILIDAD (3 registros)
-- ===========================================================
INSERT INTO tipos_vulnerabilidad (nombre, descripcion)
SELECT * FROM (
    SELECT 'Adulto mayor', 'Paciente mayor de 65 años' UNION ALL
    SELECT 'Embarazada', 'Paciente en estado de gestación' UNION ALL
    SELECT 'Discapacidad', 'Paciente con discapacidad física o mental'
) AS nuevas
WHERE NOT EXISTS (SELECT 1 FROM tipos_vulnerabilidad);


-- ===========================================================
-- 3. SOLICITUDES FICTICIAS (6 registros)
-- ===========================================================
-- Cobertura de escenarios:
--   #1 GES en espera (prioridad 1, sin fecha_cita)
--   #2 URGENTE vulnerable en espera (prioridad 2, sin fecha_cita)
--   #3 Vulnerable embarazada CITADA (prioridad 3, fecha_cita +7d)
--   #4 ELECTIVA común CITADA (prioridad 4, fecha_cita +15d)
--   #5 GES ATENDIDA (cita ya pasó, -5d)
--   #6 URGENTE ANULADA con motivo (sin fecha_cita)
-- ===========================================================

INSERT INTO solicitudes (
    rut_paciente, rut_funcionario, especialidad_id, diagnostico,
    es_g_e_s, patologia_g_e_s, nivel_urgencia,
    es_vulnerable, tipo_vulnerabilidad_id, prioridad, estado,
    fecha_registro, fecha_actualizacion, fecha_cita
)
SELECT * FROM (
    SELECT
        '12345678-9'   AS rut_paciente,
        '11111111-1'   AS rut_funcionario,
        1              AS especialidad_id,
        'Dolor torácico crónico con sospecha de cardiopatía isquémica' AS diagnostico,
        true           AS es_g_e_s,
        'Infarto agudo al miocardio' AS patologia_g_e_s,
        'GES'          AS nivel_urgencia,
        false          AS es_vulnerable,
        NULL           AS tipo_vulnerabilidad_id,
        1              AS prioridad,
        'EN_ESPERA'    AS estado,
        DATE_SUB(NOW(), INTERVAL 10 DAY) AS fecha_registro,
        DATE_SUB(NOW(), INTERVAL 10 DAY) AS fecha_actualizacion,
        NULL           AS fecha_cita
    UNION ALL
    SELECT
        '98765432-1', '11111111-1', 2,
        'Dificultad respiratoria severa en paciente vulnerable',
        false, NULL, 'URGENTE',
        true, 1, 2, 'EN_ESPERA',
        DATE_SUB(NOW(), INTERVAL 8 DAY),
        DATE_SUB(NOW(), INTERVAL 8 DAY),
        NULL
    UNION ALL
    SELECT
        '11222333-4', '11111111-1', 3,
        'Esguince severo en paciente embarazada',
        false, NULL, 'ELECTIVA',
        true, 2, 3, 'CITADO',
        DATE_SUB(NOW(), INTERVAL 15 DAY),
        DATE_SUB(NOW(), INTERVAL 2 DAY),
        DATE_ADD(NOW(), INTERVAL 7 DAY)
    UNION ALL
    SELECT
        '22333444-5', '11111111-1', 4,
        'Cefaleas recurrentes sin causa identificada',
        false, NULL, 'ELECTIVA',
        false, NULL, 4, 'CITADO',
        DATE_SUB(NOW(), INTERVAL 20 DAY),
        DATE_SUB(NOW(), INTERVAL 5 DAY),
        DATE_ADD(NOW(), INTERVAL 15 DAY)
    UNION ALL
    SELECT
        '33444555-6', '11111111-1', 1,
        'Arritmia cardíaca diagnosticada en control rutinario',
        true, 'Arritmia cardíaca confirmada', 'GES',
        false, NULL, 1, 'ATENDIDO',
        DATE_SUB(NOW(), INTERVAL 30 DAY),
        DATE_SUB(NOW(), INTERVAL 5 DAY),
        DATE_SUB(NOW(), INTERVAL 5 DAY)
    UNION ALL
    SELECT
        '44555666-7', '11111111-1', 3,
        'Esguince severo de tobillo derecho',
        false, NULL, 'URGENTE',
        false, NULL, 2, 'ANULADO',
        DATE_SUB(NOW(), INTERVAL 12 DAY),
        DATE_SUB(NOW(), INTERVAL 11 DAY),
        NULL
) AS nuevas
WHERE NOT EXISTS (SELECT 1 FROM solicitudes);


-- ===========================================================
-- 4. HISTORIAL DE ESTADOS (12 registros)
-- ===========================================================
-- Una entrada inicial (EN_ESPERA) por cada solicitud,
-- más las transiciones posteriores cuando aplican.
-- Los IDs 1..6 corresponden a las solicitudes insertadas arriba
-- (asumiendo BD limpia).
-- ===========================================================

INSERT INTO historial_estados (
    solicitud_id, estado_anterior, estado_nuevo, motivo,
    fecha_cambio, rut_usuario_responsable
)
SELECT * FROM (
    -- Solicitud #1: EN_ESPERA (1 entrada)
    SELECT
        1            AS solicitud_id,
        NULL         AS estado_anterior,
        'EN_ESPERA'  AS estado_nuevo,
        NULL         AS motivo,
        DATE_SUB(NOW(), INTERVAL 10 DAY) AS fecha_cambio,
        '11111111-1' AS rut_usuario_responsable
    UNION ALL
    -- Solicitud #2: EN_ESPERA (1 entrada)
    SELECT 2, NULL, 'EN_ESPERA', NULL,
           DATE_SUB(NOW(), INTERVAL 8 DAY), '11111111-1'
    UNION ALL
    -- Solicitud #3: EN_ESPERA → CITADO (2 entradas)
    SELECT 3, NULL, 'EN_ESPERA', NULL,
           DATE_SUB(NOW(), INTERVAL 15 DAY), '11111111-1'
    UNION ALL
    SELECT 3, 'EN_ESPERA', 'CITADO', NULL,
           DATE_SUB(NOW(), INTERVAL 2 DAY), '11111111-1'
    UNION ALL
    -- Solicitud #4: EN_ESPERA → CITADO (2 entradas)
    SELECT 4, NULL, 'EN_ESPERA', NULL,
           DATE_SUB(NOW(), INTERVAL 20 DAY), '11111111-1'
    UNION ALL
    SELECT 4, 'EN_ESPERA', 'CITADO', NULL,
           DATE_SUB(NOW(), INTERVAL 5 DAY), '11111111-1'
    UNION ALL
    -- Solicitud #5: EN_ESPERA → CITADO → ATENDIDO (3 entradas)
    SELECT 5, NULL, 'EN_ESPERA', NULL,
           DATE_SUB(NOW(), INTERVAL 30 DAY), '11111111-1'
    UNION ALL
    SELECT 5, 'EN_ESPERA', 'CITADO', NULL,
           DATE_SUB(NOW(), INTERVAL 20 DAY), '11111111-1'
    UNION ALL
    SELECT 5, 'CITADO', 'ATENDIDO', NULL,
           DATE_SUB(NOW(), INTERVAL 5 DAY), '11111111-1'
    UNION ALL
    -- Solicitud #6: EN_ESPERA → ANULADO con motivo (2 entradas)
    SELECT 6, NULL, 'EN_ESPERA', NULL,
           DATE_SUB(NOW(), INTERVAL 12 DAY), '11111111-1'
    UNION ALL
    SELECT 6, 'EN_ESPERA', 'ANULADO',
           'Paciente desistió por motivos personales',
           DATE_SUB(NOW(), INTERVAL 11 DAY), '11111111-1'
) AS h
WHERE NOT EXISTS (SELECT 1 FROM historial_estados);
