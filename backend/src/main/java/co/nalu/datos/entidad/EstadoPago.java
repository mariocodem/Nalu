package co.nalu.datos.entidad;

/** Estados de un intento de pago; corresponden a los de la transacción de Wompi. */
public enum EstadoPago {
    INICIADO, PENDIENTE, APROBADO, RECHAZADO, ANULADO, ERROR;

    public boolean esFinal() {
        return this == APROBADO || this == RECHAZADO || this == ANULADO || this == ERROR;
    }
}
