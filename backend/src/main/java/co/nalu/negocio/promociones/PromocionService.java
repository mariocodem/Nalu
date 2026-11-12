package co.nalu.negocio.promociones;

import co.nalu.datos.entidad.Promocion;
import co.nalu.datos.repositorio.PromocionRepository;
import co.nalu.negocio.comun.ConflictoException;
import co.nalu.negocio.comun.RecursoNoEncontradoException;
import co.nalu.negocio.promociones.dto.PromocionDtos;
import co.nalu.negocio.promociones.dto.PromocionDtos.PromocionRequest;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** RF11: cupones (administración y validación en el checkout). */
@Service
public class PromocionService {

    /** Resultado de aplicar un cupón: si no aplica, {@code promocion} es null y {@code mensaje} explica por qué. */
    public record CuponAplicado(Promocion promocion, BigDecimal descuento, String mensaje) {}

    private final PromocionRepository promociones;

    public PromocionService(PromocionRepository promociones) {
        this.promociones = promociones;
    }

    @Transactional(readOnly = true)
    public List<PromocionDtos.Promocion> listar() {
        return promociones.findAllByOrderByFechaFinDesc().stream().map(PromocionService::dto).toList();
    }

    @Transactional
    public PromocionDtos.Promocion crear(PromocionRequest s) {
        if (promociones.existsByCodigo(s.codigo())) {
            throw new ConflictoException("Ya existe el cupón " + s.codigo());
        }
        var p = new Promocion(s.codigo(), s.tipo(), s.valor(), s.fechaInicio(), s.fechaFin(), s.usosMaximos());
        p.actualizar(s.tipo(), s.valor(), s.fechaInicio(), s.fechaFin(), s.usosMaximos(), s.activa());
        return dto(promociones.save(p));
    }

    /** El código no se cambia: puede estar en pedidos ya hechos. */
    @Transactional
    public PromocionDtos.Promocion actualizar(long id, PromocionRequest s) {
        var p = promociones.findById(id).orElseThrow(() -> new RecursoNoEncontradoException("El cupón no existe"));
        p.actualizar(s.tipo(), s.valor(), s.fechaInicio(), s.fechaFin(), s.usosMaximos(), s.activa());
        return dto(p);
    }

    /** Valida un cupón contra el subtotal (plan §6.3 paso 3). Un código vacío no es un error. */
    @Transactional(readOnly = true)
    public CuponAplicado aplicar(String codigo, BigDecimal subtotal, LocalDate hoy) {
        if (codigo == null || codigo.isBlank()) {
            return new CuponAplicado(null, BigDecimal.ZERO, null);
        }
        var promocion = promociones.findByCodigo(codigo.strip().toUpperCase(Locale.ROOT));
        if (promocion.isEmpty()) {
            return new CuponAplicado(null, BigDecimal.ZERO, "El cupón no existe");
        }
        var motivo = promocion.get().motivoNoAplicable(hoy);
        if (motivo != null) {
            return new CuponAplicado(null, BigDecimal.ZERO, motivo);
        }
        return new CuponAplicado(promocion.get(), promocion.get().descuentoPara(subtotal), null);
    }

    private static PromocionDtos.Promocion dto(Promocion p) {
        return new PromocionDtos.Promocion(p.getId(), p.getCodigo(), p.getTipo(), p.getValor(), p.getFechaInicio(),
                p.getFechaFin(), p.getUsosMaximos(), p.getUsosActuales(), p.isActiva());
    }
}
