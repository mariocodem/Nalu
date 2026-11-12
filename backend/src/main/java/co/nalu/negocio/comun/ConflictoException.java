package co.nalu.negocio.comun;

/**
 * La operación choca con el estado actual de los datos: existencias insuficientes, transición de
 * estado no permitida, valor único repetido… La API responde 409.
 *
 * <p>El mensaje se muestra al usuario final: debe estar en español y no contener datos internos.
 */
public class ConflictoException extends RuntimeException {

    public ConflictoException(String mensaje) {
        super(mensaje);
    }
}
