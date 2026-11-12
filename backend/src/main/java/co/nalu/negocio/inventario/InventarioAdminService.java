package co.nalu.negocio.inventario;

import co.nalu.datos.entidad.Administrador;
import co.nalu.datos.entidad.MovimientoInventario;
import co.nalu.datos.entidad.TipoMovimiento;
import co.nalu.datos.repositorio.MovimientoInventarioRepository;
import co.nalu.datos.repositorio.UsuarioRepository;
import co.nalu.datos.repositorio.VarianteProductoRepository;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.negocio.inventario.dto.InventarioDtos.Alerta;
import co.nalu.negocio.inventario.dto.InventarioDtos.Movimiento;
import co.nalu.negocio.inventario.dto.InventarioDtos.MovimientoRequest;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF13 / CU-12: movimientos manuales de inventario y alertas (plan §6.7). */
@Service
public class InventarioAdminService {

    private final InventarioService inventario;
    private final MovimientoInventarioRepository movimientos;
    private final VarianteProductoRepository variantes;
    private final UsuarioRepository usuarios;

    public InventarioAdminService(InventarioService inventario, MovimientoInventarioRepository movimientos,
                                  VarianteProductoRepository variantes, UsuarioRepository usuarios) {
        this.inventario = inventario;
        this.movimientos = movimientos;
        this.variantes = variantes;
        this.usuarios = usuarios;
    }

    /** Bloquea la variante y no permite retirar unidades ya reservadas por pedidos en curso. */
    @Transactional
    public Movimiento registrar(MovimientoRequest solicitud, long idAdministrador) {
        var variante = inventario.bloquear(Set.of(solicitud.idVariante())).get(solicitud.idVariante());
        if (variante == null) {
            throw new RecursoNoEncontradoException("La variante no existe");
        }
        int delta = solicitud.tipo() == TipoMovimiento.SALIDA ? -solicitud.cantidad() : solicitud.cantidad();
        try {
            variante.ajustar(delta);
        } catch (IllegalStateException e) {
            throw new ConflictoException("No se puede: quedarían " + (variante.getStockActual() + delta)
                    + " unidades y hay " + variante.getStockReservado() + " reservadas por pedidos en curso");
        }
        var admin = usuarios.findById(idAdministrador).filter(Administrador.class::isInstance).map(Administrador.class::cast)
                .orElseThrow(() -> new RecursoNoEncontradoException("El administrador no existe"));
        return dto(movimientos.save(new MovimientoInventario(variante, admin, solicitud.tipo(), delta, solicitud.motivo())));
    }

    @Transactional(readOnly = true)
    public Pagina<Movimiento> listar(Long idVariante, Pageable pagina) {
        var resultado = idVariante == null ? movimientos.findAll(pagina) : movimientos.findByVarianteId(idVariante, pagina);
        return Pagina.de(resultado, InventarioAdminService::dto);
    }

    @Transactional(readOnly = true)
    public List<Alerta> alertas(int umbral) {
        return variantes.conPocasExistencias(umbral).stream()
                .map(v -> new Alerta(v.getId(), v.getProducto().getId(), v.getSku(), v.getProducto().getNombre(), v.getTalla(),
                        v.getStockActual(), v.getStockReservado(), v.disponible()))
                .toList();
    }

    private static Movimiento dto(MovimientoInventario m) {
        var v = m.getVariante();
        var responsable = m.getAdministrador() == null ? "Sistema"
                : m.getAdministrador().getNombre() + " " + m.getAdministrador().getApellido();
        return new Movimiento(m.getId(), m.getFecha(), v.getId(), v.getSku(), v.getProducto().getNombre(), v.getTalla(),
                m.getTipo(), m.getCantidad(), m.getMotivo(), responsable,
                m.getPedido() == null ? null : m.getPedido().getNumeroPedido());
    }
}
