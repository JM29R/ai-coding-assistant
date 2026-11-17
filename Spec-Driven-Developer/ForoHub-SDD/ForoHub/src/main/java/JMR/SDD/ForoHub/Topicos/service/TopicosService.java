package JMR.SDD.ForoHub.Topicos.service;

import JMR.SDD.ForoHub.Topicos.controller.TopicoDTORequest;
import JMR.SDD.ForoHub.Topicos.controller.TopicoDTOResponse;
import JMR.SDD.ForoHub.Topicos.entity.EstadoTopico;
import JMR.SDD.ForoHub.Topicos.entity.Topico;
import JMR.SDD.ForoHub.Topicos.entity.TopicoRepository;
import JMR.SDD.ForoHub.respuestas.service.RespuestasService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reglas de negocio del modulo de topicos (R1-R5).
 */
@Service
@Transactional
public class TopicosService {

    private final TopicoRepository topicoRepository;
    private final TopicoMapper topicoMapper;
    private final RespuestasService respuestasService;

    public TopicosService(
            TopicoRepository topicoRepository,
            TopicoMapper topicoMapper,
            RespuestasService respuestasService) {
        this.topicoRepository = topicoRepository;
        this.topicoMapper = topicoMapper;
        this.respuestasService = respuestasService;
    }

    /**
     * RF1: crea un topico ACTIVO con fecha de creacion actual (R1),
     * validando obligatorios y duplicados activos (R2).
     */
    public TopicoDTOResponse crear(TopicoDTORequest request) {
        validarObligatorio(request.titulo(), "El titulo es obligatorio");
        validarObligatorio(request.mensaje(), "El mensaje es obligatorio");
        validarObligatorio(request.autor(), "El autor es obligatorio");

        Topico topico = topicoMapper.toDomain(request);
        verificarDuplicado(topico.getTitulo(), topico.getMensaje());

        topico.setEstado(EstadoTopico.ACTIVO);
        topico.setFechaCreacion(LocalDateTime.now());

        Topico guardado = topicoRepository.save(topico);
        return topicoMapper.toResponse(guardado);
    }

    /**
     * RF2: lista todos los topicos ACTIVO, mas recientes primero (R5).
     */
    @Transactional(readOnly = true)
    public List<TopicoDTOResponse> listar() {
        return topicoRepository.findByEstadoOrderByFechaCreacionDesc(EstadoTopico.ACTIVO)
                .stream()
                .map(topicoMapper::toResponse)
                .toList();
    }

    /**
     * RF3: detalle de un topico ACTIVO; inexistente o inactivo -> 404 (R5).
     */
    @Transactional(readOnly = true)
    public TopicoDTOResponse obtenerPorId(Long id) {
        return topicoMapper.toResponse(buscarActivoPorId(id));
    }

    /**
     * RF4: actualizacion parcial (R3). Solo se aplican los campos con valor;
     * los campos null, vacios o con solo espacios conservan el valor de la BD.
     * R2 se verifica sobre los valores finales excluyendo el propio topico.
     */
    public TopicoDTOResponse actualizar(Long id, TopicoDTORequest request) {
        Topico topico = buscarActivoPorId(id);

        boolean huboCambios = false;
        if (tieneValor(request.titulo())) {
            topico.setTitulo(request.titulo().trim());
            huboCambios = true;
        }
        if (tieneValor(request.mensaje())) {
            topico.setMensaje(request.mensaje().trim());
            huboCambios = true;
        }
        if (tieneValor(request.autor())) {
            topico.setAutor(request.autor().trim());
            huboCambios = true;
        }

        if (!huboCambios) {
            return topicoMapper.toResponse(topico);
        }

        verificarDuplicadoExcluyendoId(topico.getTitulo(), topico.getMensaje(), id);

        Topico actualizado = topicoRepository.save(topico);
        return topicoMapper.toResponse(actualizado);
    }

    /**
     * RF5: borrado logico; la fila permanece con estado INACTIVO (R4).
     * R6: en la misma transaccion inactiva las respuestas activas del topico.
     */
    public void eliminar(Long id) {
        Topico topico = buscarActivoPorId(id);
        topico.setEstado(EstadoTopico.INACTIVO);
        topicoRepository.save(topico);

        respuestasService.inactivarPorTopico(id);
    }

    private Topico buscarActivoPorId(Long id) {
        return topicoRepository.findByIdAndEstado(id, EstadoTopico.ACTIVO)
                .orElseThrow(() -> new TopicoNoEncontradoException(id));
    }

    private void verificarDuplicado(String titulo, String mensaje) {
        boolean existe = topicoRepository.existsByTituloIgnoreCaseAndMensajeIgnoreCaseAndEstado(
                titulo, mensaje, EstadoTopico.ACTIVO);
        if (existe) {
            throw new ValidacionTopicoException(
                    "Ya existe un topico activo con el mismo titulo y mensaje");
        }
    }

    private void verificarDuplicadoExcluyendoId(String titulo, String mensaje, Long id) {
        boolean existe = topicoRepository.existsByTituloIgnoreCaseAndMensajeIgnoreCaseAndEstadoAndIdNot(
                titulo, mensaje, EstadoTopico.ACTIVO, id);
        if (existe) {
            throw new ValidacionTopicoException(
                    "Ya existe un topico activo con el mismo titulo y mensaje");
        }
    }

    private void validarObligatorio(String valor, String mensaje) {
        if (!tieneValor(valor)) {
            throw new ValidacionTopicoException(mensaje);
        }
    }

    private boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }
}
