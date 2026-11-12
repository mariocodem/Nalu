package co.nalu.datos.entidad;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/**
 * Pruebas unitarias del árbol de {@link Categoria}, sin Spring ni base de datos.
 */
class CategoriaTest {

    @Test
    void moverAUnaCategoriaSobreSiMismaFalla() {
        Categoria raiz = new Categoria("Anillos", "anillos", null, null);

        assertThatThrownBy(() -> raiz.moverA(raiz))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(raiz.getPadre()).isNull();
    }

    @Test
    void moverAUnaDescendienteFalla() {
        Categoria raiz = new Categoria("Anillos", "anillos", null, null);
        Categoria hija = new Categoria("Solitarios", "solitarios", null, raiz);
        Categoria nieta = new Categoria("Oro", "oro", null, hija);
        raiz.getHijos().add(hija);
        hija.getHijos().add(nieta);

        assertThatThrownBy(() -> raiz.moverA(nieta))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(raiz.getPadre()).isNull();
        assertThat(nieta.getPadre()).isSameAs(hija);
    }

    @Test
    void moverAOtraRamaFunciona() {
        Categoria anillos = new Categoria("Anillos", "anillos", null, null);
        Categoria solitarios = new Categoria("Solitarios", "solitarios", null, anillos);
        Categoria ofertas = new Categoria("Ofertas", "ofertas", null, null);

        solitarios.moverA(ofertas);

        assertThat(solitarios.getPadre()).isSameAs(ofertas);
    }

    @Test
    void moverAUnaRaizFunciona() {
        Categoria anillos = new Categoria("Anillos", "anillos", null, null);
        Categoria solitarios = new Categoria("Solitarios", "solitarios", null, anillos);

        solitarios.moverA(null);

        assertThat(solitarios.getPadre()).isNull();
    }
}
