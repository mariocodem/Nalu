package co.nalu;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;

/** Eventos de Wompi con la estructura y la firma documentadas, firmados con el secreto de pruebas. */
public final class EventosWompi {

    public static final String SECRETO_EVENTOS = "test_events_nalu";

    private EventosWompi() {}

    public static String evento(String idTransaccion, String referencia, String estado, long centavos) {
        long timestamp = 1_762_700_000L;
        var checksum = sha256(idTransaccion + estado + centavos + timestamp + SECRETO_EVENTOS);
        return """
                {"event":"transaction.updated",
                 "data":{"transaction":{"id":"%s","amount_in_cents":%d,"reference":"%s","currency":"COP",
                         "payment_method_type":"CARD","status":"%s"}},
                 "environment":"test",
                 "signature":{"properties":["transaction.id","transaction.status","transaction.amount_in_cents"],
                              "checksum":"%s"},
                 "timestamp":%d,"sent_at":"2026-11-09T15:00:00.000Z"}"""
                .formatted(idTransaccion, centavos, referencia, estado, checksum.toUpperCase(), timestamp);
    }

    public static String sha256(String texto) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
