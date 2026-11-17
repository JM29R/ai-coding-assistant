package JMR.SDD.ForoHub.respuestas.service;

import JMR.SDD.ForoHub.Topicos.entity.EstadoTopico;
import JMR.SDD.ForoHub.Topicos.entity.Topico;
import JMR.SDD.ForoHub.Topicos.entity.TopicoRepository;
import JMR.SDD.ForoHub.Topicos.service.TopicoNoEncontradoException;
import JMR.SDD.ForoHub.respuestas.controller.RespuestaDTORequest;
import JMR.SDD.ForoHub.respuestas.controller.RespuestaDTOResponse;
import JMR.SDD.ForoHub.respuestas.entity.EstadoRespuesta;
import JMR.SDD.ForoHub.respuestas.entity.Respuesta;
import JMR.SDD.ForoHub.respuestas.entity.RespuestaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Reglas de negocio del modulo de respuestas (R1-R6).
 */
@Service
@Transactional
public class RespuestasService {

    private final RespuestaRepository respuestaRepository;
    private final TopicoRepository topicoRepository;
    private final RespuestaMapper respuestaMapper;

    public RespuestasService(
            RespuestaRepository respuestaRepository,
            TopicoRepository topicoRepository,
            RespuestaMapper respuestaMapper) {
        this.respuestaRepository = respuestaRepository;
        this.topicoRepository = topicoRepository;
        this.respuestaMapper = respuestaMapper;
    }

    /**
     * R1-R2: crea una respuesta ACTIVO con fecha de creacion actual,
     * validando obligatorios y que el topico exista y este ACTIVO.
     */
    public RespuestaDTOResponse crear(RespuestaDTORequest request) {
        if (request == null) {
            throw new ValidacionRespuestaException("Los datos de la respuesta son obligatorios");
        }
        validarObligatorio(request.mensaje(), "El mensaje es obligatorio");
        validarObligatorio(request.autor(), "El autor es obligatorio");
        if (request.topicoId() == null) {
            throw new ValidacionRespuestaException("El topicoId es obligatorio");
        }

        Topico topico = buscarTopicoActivo(request.topicoId());

        Respuesta respuesta = respuestaMapper.toDomain(request);
        respuesta.setTopico(topico);
        respuesta.setEstado(EstadoRespuesta.ACTIVO);
        respuesta.setFechaCreacion(LocalDateTime.now());

        Respuesta guardada = respuestaRepository.save(respuesta);
        return respuestaMapper.toResponse(guardada);
    }

    /**
     * R5: lista las respuestas ACTIVO de un topico activo, mas recientes primero.
     * Topico inexistente o inactivo -> 404.
     */
    @Transactional(readOnly = true)
    public List<RespuestaDTOResponse> listarPorTopico(Long topicoId) {
        buscarTopicoActivo(topicoId);

        return respuestaRepository
                .findByTopicoIdAndEstadoOrderByFechaCreacionDesc(topicoId, EstadoRespuesta.ACTIVO)
                .stream()
                .map(respuestaMapper::toResponse)
                .toList();
    }

    /**
     * R5: detalle de una respuesta ACTIVO; inexistente o inactiva -> 404.
     */
    @Transactional(readOnly = true)
    public RespuestaDTOResponse obtenerPorId(Long id) {
        return respuestaMapper.toResponse(buscarActivaPorId(id));
    }

    /**
     * R3: actualizacion parcial; solo se modifica el mensaje cuando llega con
     * valor. Un request nulo o un mensaje nulo, vacio o con solo espacios
     * conserva el valor de la BD. Autor y topicoId se ignoran siempre.
     */
    public RespuestaDTOResponse actualizar(Long id, RespuestaDTORequest request) {
        Respuesta respuesta = buscarActivaPorId(id);

        if (request != null && tieneValor(request.mensaje())) {
            respuesta.setMensaje(request.mensaje().trim());
            respuestaRepository.save(respuesta);
        }

        return respuestaMapper.toResponse(respuesta);
    }

    /**
     * R4: borrado logico; la fila permanece con estado INACTIVO.
     */
    public void eliminar(Long id) {
        Respuesta respuesta = buscarActivaPorId(id);
        respuesta.setEstado(EstadoRespuesta.INACTIVO);
        respuestaRepository.save(respuesta);
    }

    /**
     * R6: cascade logico; inactiva todas las respuestas ACTIVO del topico.
     * Sin 404: si no hay respuestas activas, termina sin hacer nada.
     */
    public void inactivarPorTopico(Long topicoId) {
        List<Respuesta> activas = respuestaRepository
                .findByTopicoIdAndEstado(topicoId, EstadoRespuesta.ACTIVO);

        activas.forEach(respuesta -> respuesta.setEstado(EstadoRespuesta.INACTIVO));
        respuestaRepository.saveAll(activas);
    }

    private Respuesta buscarActivaPorId(Long id) {
        return respuestaRepository.findByIdAndEstado(id, EstadoRespuesta.ACTIVO)
                .orElseThrow(() -> new RespuestaNoEncontradaException(id));
    }

    private Topico buscarTopicoActivo(Long topicoId) {
        return topicoRepository.findByIdAndEstado(topicoId, EstadoTopico.ACTIVO)
                .orElseThrow(() -> new TopicoNoEncontradoException(topicoId));
    }

    private void validarObligatorio(String valor, String mensaje) {
        if (!tieneValor(valor)) {
            throw new ValidacionRespuestaException(mensaje);
        }
    }

    private boolean tieneValor(String valor) {
        return valor != null && !valor.isBlank();
    }
}
