-- Migracion del modulo de Respuestas (seccion 3 de la especificacion).
-- Asume que V1__crear_tabla_topicos.sql ya fue aplicada.
-- La FK incluye ON DELETE CASCADE como respaldo fisico; el borrado de
-- aplicacion es logico (estado INACTIVO).
CREATE TABLE respuestas (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    mensaje TEXT NOT NULL,
    fecha_creacion DATETIME NOT NULL,
    estado VARCHAR(20) NOT NULL,
    autor VARCHAR(100) NOT NULL,
    topico_id BIGINT NOT NULL,
    CONSTRAINT fk_respuestas_topico
        FOREIGN KEY (topico_id) REFERENCES topicos (id) ON DELETE CASCADE
);
