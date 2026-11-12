package co.nalu.negocio.cuentas;

import co.nalu.datos.repositorio.IntentoLoginRepository;
import co.nalu.datos.repositorio.RefreshTokenRepository;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Limpieza diaria: refresh tokens expirados hace más de 30 días (plan §8.3, regla 4) e intentos de
 * login de más de 30 días, que ya no sirven para el bloqueo ni para la auditoría reciente.
 */
@Component
public class LimpiezaDatosCuentas {

    private static final Logger log = LoggerFactory.getLogger(LimpiezaDatosCuentas.class);
    static final int DIAS_DE_RETENCION = 30;

    private final RefreshTokenRepository refreshTokens;
    private final IntentoLoginRepository intentos;
    private final Clock reloj;

    public LimpiezaDatosCuentas(RefreshTokenRepository refreshTokens, IntentoLoginRepository intentos, Clock reloj) {
        this.refreshTokens = refreshTokens;
        this.intentos = intentos;
        this.reloj = reloj;
    }

    @Scheduled(cron = "0 30 3 * * *", zone = "America/Bogota")
    @Transactional
    public void limpiar() {
        var limite = OffsetDateTime.now(reloj).minusDays(DIAS_DE_RETENCION);
        int tokens = refreshTokens.eliminarExpiradosAntesDe(limite);
        int intentosEliminados = intentos.eliminarAnterioresA(limite);
        log.info("Limpieza de cuentas: {} refresh tokens y {} intentos de login eliminados", tokens, intentosEliminados);
    }
}
