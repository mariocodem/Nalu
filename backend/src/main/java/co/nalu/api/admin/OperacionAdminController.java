package co.nalu.api.admin;

import static co.nalu.api.error.Validacion.validar;

import co.nalu.api.Paginacion;
import co.nalu.api.UsuarioActual;
import co.nalu.negocio.auditoria.AuditoriaService;
import co.nalu.negocio.comun.Pagina;
import co.nalu.negocio.contenido.ResenaService;
import co.nalu.negocio.contenido.dto.ResenaDtos.FiltroResenas;
import co.nalu.negocio.contenido.dto.ResenaDtos.ModerarRequest;
import co.nalu.negocio.contenido.dto.ResenaDtos.ResenaAdmin;
import co.nalu.negocio.inventario.InventarioAdminService;
import co.nalu.negocio.inventario.dto.InventarioDtos.Alerta;
import co.nalu.negocio.inventario.dto.InventarioDtos.Movimiento;
import co.nalu.negocio.inventario.dto.InventarioDtos.MovimientoRequest;
import co.nalu.negocio.reportes.ReporteService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** RF13 (inventario), RF07 (moderación de reseñas) y RF15 (reportes). Contrato: api-fase5.md §2–§4. */
@RestController
@RequestMapping("/api/v1/admin")
public class OperacionAdminController {

    private final InventarioAdminService inventario;
    private final ResenaService resenas;
    private final ReporteService reportes;
    private final AuditoriaService auditoria;

    public OperacionAdminController(InventarioAdminService inventario, ResenaService resenas, ReporteService reportes,
                                    AuditoriaService auditoria) {
        this.auditoria = auditoria;
        this.inventario = inventario;
        this.resenas = resenas;
        this.reportes = reportes;
    }

    @PostMapping("/inventario/movimientos")
    @ResponseStatus(HttpStatus.CREATED)
    Movimiento registrar(@RequestBody MovimientoRequest solicitud, @AuthenticationPrincipal Jwt jwt) {
        return inventario.registrar(validar(solicitud, MovimientoRequest.ESQUEMA), UsuarioActual.id(jwt));
    }

    @GetMapping("/inventario/movimientos")
    Pagina<Movimiento> movimientos(@RequestParam(required = false) Long idVariante,
                                   @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "30") int size) {
        return inventario.listar(idVariante, Paginacion.de(page, size, null, Set.of(), Sort.by(Sort.Direction.DESC, "fecha", "id")));
    }

    @GetMapping("/inventario/alertas")
    List<Alerta> alertas(@RequestParam(defaultValue = "2") int umbral) {
        return inventario.alertas(Math.clamp(umbral, 0, 1000));
    }

    @GetMapping("/resenas")
    Pagina<ResenaAdmin> resenas(FiltroResenas filtro,
                                @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return resenas.listar(filtro.estado(), Paginacion.de(page, size, null, Set.of(), Sort.by(Sort.Direction.DESC, "fecha")));
    }

    @PatchMapping("/resenas/{id}")
    ResenaAdmin moderar(@PathVariable long id, @RequestBody ModerarRequest solicitud) {
        return resenas.moderar(id, solicitud.estado());
    }

    @GetMapping("/auditoria")
    Pagina<AuditoriaService.Registro> auditoria(@RequestParam(required = false) String recurso,
                                                @RequestParam(required = false) String idRecurso,
                                                @RequestParam(defaultValue = "0") int page,
                                                @RequestParam(defaultValue = "50") int size) {
        return auditoria.listar(recurso, idRecurso, Paginacion.de(page, size, null, Set.of(),
                Sort.by(Sort.Direction.DESC, "fecha", "id")));
    }

    /** {@code formato=csv} descarga el archivo; por defecto responde JSON. */
    @GetMapping("/reportes/{tipo}")
    ResponseEntity<?> reporte(@PathVariable String tipo,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
                              @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
                              @RequestParam(defaultValue = "json") String formato) {
        var tabla = reportes.generar(tipo, desde, hasta);
        if (!formato.equals("csv")) {
            return ResponseEntity.ok(tabla);
        }
        var nombre = "nalu-%s-%s-a-%s.csv".formatted(tipo, desde, hasta);
        return ResponseEntity.ok()
                .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombre + "\"")
                .body(ReporteService.csv(tabla));
    }
}
