-- Especialidades
INSERT INTO especialidades (nombre, descripcion, activo)
SELECT * FROM (
    SELECT 'Cardiología', 'Especialidad del corazón', true UNION ALL
    SELECT 'Broncopulmonar', 'Especialidad del sistema respiratorio', true UNION ALL
    SELECT 'Traumatología', 'Especialidad del sistema músculo esquelético', true UNION ALL
    SELECT 'Neurología', 'Especialidad del sistema nervioso', true
) AS nuevas
WHERE NOT EXISTS (SELECT 1 FROM especialidades);

-- Tipos de vulnerabilidad
INSERT INTO tipos_vulnerabilidad (nombre, descripcion)
SELECT * FROM (
    SELECT 'Adulto mayor', 'Paciente mayor de 65 años' UNION ALL
    SELECT 'Embarazada', 'Paciente en estado de gestación' UNION ALL
    SELECT 'Discapacidad', 'Paciente con discapacidad física o mental'
) AS nuevas
WHERE NOT EXISTS (SELECT 1 FROM tipos_vulnerabilidad);