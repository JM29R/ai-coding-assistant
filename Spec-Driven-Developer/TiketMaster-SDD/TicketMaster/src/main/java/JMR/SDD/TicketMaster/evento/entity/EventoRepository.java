package JMR.SDD.TicketMaster.evento.entity;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface EventoRepository extends JpaRepository<Evento, Long> {

    /**
     * Consulta única con todos los filtros opcionales. Un parámetro {@code null} se ignora.
     *
     * <ul>
     *   <li>Sin filtros de estado se excluyen los {@code ELIMINADO}.</li>
     *   <li>Con {@code estadoDeclarado} se devuelve solo ese estado (incluye {@code ELIMINADO}).</li>
     *   <li>Con {@code estadoEfectivo} se aplica el cálculo sobre las fechas usando {@code ahora}.</li>
     *   <li>{@code desde}/{@code hasta} filtran por solapamiento del evento con el rango.</li>
     * </ul>
     *
     * <p>No se usa {@code NOW()} de MySQL: el instante se recibe como parámetro para que
     * el filtro y la respuesta compartan el mismo reloj.</p>
     */
    @Query("""
            SELECT e FROM Evento e
            WHERE (:estadoDeclarado IS NULL OR e.estado = :estadoDeclarado)
              AND (:estadoDeclarado IS NOT NULL
                   OR e.estado <> JMR.SDD.TicketMaster.evento.entity.EventoEstado.ELIMINADO)
              AND (:estadoEfectivo IS NULL OR (
                    (:estadoEfectivo = 'PUBLICADO'
                        AND e.estado = JMR.SDD.TicketMaster.evento.entity.EventoEstado.PUBLICADO
                        AND :ahora < e.inicio)
                 OR (:estadoEfectivo = 'EN_CURSO'
                        AND e.estado = JMR.SDD.TicketMaster.evento.entity.EventoEstado.PUBLICADO
                        AND :ahora BETWEEN e.inicio AND e.fin)
                 OR (:estadoEfectivo = 'FINALIZADO'
                        AND e.estado = JMR.SDD.TicketMaster.evento.entity.EventoEstado.PUBLICADO
                        AND :ahora > e.fin)
              ))
              AND (:desde IS NULL OR e.fin >= :desde)
              AND (:hasta IS NULL OR e.inicio <= :hasta)
              AND (:titulo IS NULL OR LOWER(e.titulo) LIKE CONCAT('%', LOWER(:titulo), '%'))
            ORDER BY e.inicio ASC
            """)
    List<Evento> listarConFiltros(@Param("estadoEfectivo") String estadoEfectivo,
                                 @Param("estadoDeclarado") EventoEstado estadoDeclarado,
                                 @Param("desde") LocalDateTime desde,
                                 @Param("hasta") LocalDateTime hasta,
                                 @Param("titulo") String titulo,
                                 @Param("ahora") LocalDateTime ahora);
}
