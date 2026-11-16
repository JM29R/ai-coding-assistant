-- Migracion inicial del modulo de Topicos (seccion 3 de la especificacion).
-- No se crean tablas de respuestas ni de usuarios en esta version.
CREATE TABLE topicos (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    titulo VARCHAR(150) NOT NULL,
    mensaje TEXT NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    estado VARCHAR(20) NOT NULL,
    autor VARCHAR(100) NOT NULL
);
