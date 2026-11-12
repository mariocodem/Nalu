package co.nalu.datos.entidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias de las invariantes de inventario de {@link VarianteProducto}, sin Spring ni
 * base de datos.
 */
class VarianteProductoTest {

    private final Categoria categoria = new Categoria("Anillos", "anillos", null, null);
    private final Producto producto = new Producto("AN-001", "Anillo Solitario", "anillo-solitario",
            null, categoria, new BigDecimal("189000.00"));

    private VarianteProducto variante(int existenciaInicial) {
        return new VarianteProducto(producto, "AN-001-16", "16", new BigDecimal("3.50"),
                BigDecimal.ZERO, existenciaInicial);
    }

    @Test
    void reservarMasDeLoDisponibleFallaYNoAlteraLasReservas() {
        VarianteProducto variante = variante(5);

        assertThatThrownBy(() -> variante.reservar(6))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Existencias insuficientes");
        assertThat(variante.getStockReservado()).isZero();
        assertThat(variante.disponible()).isEqualTo(5);
    }

    @Test
    void reservarDentroDeLoDisponibleDescuentaDelDisponible() {
        VarianteProducto variante = variante(5);

        variante.reservar(3);

        assertThat(variante.getStockReservado()).isEqualTo(3);
        assertThat(variante.disponible()).isEqualTo(2);
    }

    @Test
    void liberarDevuelveLasUnidadesSinBajarDeCero() {
        VarianteProducto variante = variante(5);
        variante.reservar(3);

        variante.liberar(2);

        assertThat(variante.getStockReservado()).isEqualTo(1);
        variante.liberar(10);
        assertThat(variante.getStockReservado()).isZero();
    }

    @Test
    void confirmarSalidaDescuentaReservaYExistencia() {
        VarianteProducto variante = variante(5);
        variante.reservar(2);

        variante.confirmarSalida(2);

        assertThat(variante.getStockActual()).isEqualTo(3);
        assertThat(variante.getStockReservado()).isZero();
        assertThat(variante.disponible()).isEqualTo(3);
    }

    @Test
    void ajustarQueDejaReservasSinRespaldoFalla() {
        VarianteProducto variante = variante(5);
        variante.reservar(4);

        assertThatThrownBy(() -> variante.ajustar(-2))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("reservas sin respaldo");
        assertThat(variante.getStockActual()).isEqualTo(5);
    }

    @Test
    void ajustarConRespaldoSuficienteActualizaLaExistencia() {
        VarianteProducto variante = variante(5);
        variante.reservar(4);

        variante.ajustar(-1);

        assertThat(variante.getStockActual()).isEqualTo(4);
        assertThat(variante.disponible()).isZero();
    }
}
