package co.nalu.negocio.comun;

import java.util.List;

/** 409 con el detalle de cada pieza que no alcanza (plan §6.3 paso 2). */
public class ExistenciasInsuficientesException extends ConflictoException {

    private final List<String> faltantes;

    public ExistenciasInsuficientesException(List<String> faltantes) {
        super("No hay existencias suficientes de algunas piezas de tu bolsa");
        this.faltantes = List.copyOf(faltantes);
    }

    public List<String> faltantes() {
        return faltantes;
    }
}
