package co.nalu.negocio.comun;

/**
 * El recurso no existe o no pertenece a quien lo pide. En ambos casos la API responde 404, para no
 * revelar recursos ajenos (plan de desarrollo §8.5, RNF02).
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
