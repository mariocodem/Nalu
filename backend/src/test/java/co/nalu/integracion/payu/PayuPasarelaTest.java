package co.nalu.integracion.payu;

import static co.nalu.RespuestasPayu.API_KEY;
import static co.nalu.RespuestasPayu.md5;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import co.nalu.RespuestasPayu;
import co.nalu.negocio.pagos.FirmaInvalidaException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import tools.jackson.databind.json.JsonMapper;

/** Firma del formulario y verificación de la página de respuesta de PayU WebCheckout (ADR 006). */
class PayuPasarelaTest {

    private static final String URL_SANDBOX = "https://sandbox.checkout.payulatam.com/ppp-web-gateway-payu/";

    private final PayuPasarela payu = new PayuPasarela(
            new PayuProperties("508029", "512321", API_KEY, URL_SANDBOX, true), JsonMapper.builder().build());

    @Test
    void laFirmaCoincideConElEjemploDeLaDocumentacionDePayu() {
        assertThat(PayuPasarela.md5(API_KEY + "~508029~TestPayU~3~USD")).isEqualTo("ba9ffa71559580175585e45ce70b6c37");
    }

    @Test
    void elFormularioLlevaLosCamposDeWebCheckoutYSuFirma() {
        var datos = payu.prepararPago("NALU-ABC234-1", 11_500_000, "ana@nalu.co",
                "http://localhost:4200/pago/resultado?pedido=NALU-ABC234");

        assertThat(datos.urlPago()).isEqualTo(URL_SANDBOX);
        assertThat(datos.campos())
                .containsEntry("merchantId", "508029")
                .containsEntry("accountId", "512321")
                .containsEntry("referenceCode", "NALU-ABC234-1")
                .containsEntry("amount", "115000")
                .containsEntry("tax", "0")
                .containsEntry("taxReturnBase", "0")
                .containsEntry("currency", "COP")
                .containsEntry("signature", md5(API_KEY + "~508029~NALU-ABC234-1~115000~COP"))
                .containsEntry("test", "1")
                .containsEntry("buyerEmail", "ana@nalu.co")
                .containsEntry("responseUrl", "http://localhost:4200/pago/resultado?pedido=NALU-ABC234")
                .containsKey("description")
                .doesNotContainValue(API_KEY);
    }

    @Test
    void unaRespuestaFirmadaSeTraduceAlEventoDelPuerto() {
        var evento = payu.verificarEvento(RespuestasPayu.respuesta("NALU-ABC234-1", 11_500_000, "4", "tx-1"));

        assertThat(evento.estado()).isEqualTo("APPROVED");
        assertThat(evento.referencia()).isEqualTo("NALU-ABC234-1");
        assertThat(evento.idTransaccion()).isEqualTo("tx-1");
        assertThat(evento.montoCentavos()).isEqualTo(11_500_000);
        assertThat(evento.metodo()).isEqualTo("VISA");
    }

    /** PayU firma TX_VALUE redondeado a un decimal con el método mitad al par. */
    @ParameterizedTest
    @CsvSource({"150.25, 150.2", "150.35, 150.4", "150.26, 150.3", "115000.00, 115000.0"})
    void laFirmaDeLaRespuestaRedondeaElValorAUnDecimal(String valor, String valorFirmado) {
        var cuerpo = """
                {"referenceCode":"R-1","TX_VALUE":"%s","currency":"COP","transactionState":"4",
                 "transactionId":"tx","signature":"%s"}"""
                .formatted(valor, md5(API_KEY + "~508029~R-1~" + valorFirmado + "~COP~4"));

        assertThatCode(() -> payu.verificarEvento(cuerpo)).doesNotThrowAnyException();
    }

    @Test
    void unaRespuestaAlteradaIncompletaOMalFormadaSeRechaza() {
        var rechazada = RespuestasPayu.respuesta("NALU-ABC234-1", 11_500_000, "6", "tx-2");

        assertThatThrownBy(() -> payu.verificarEvento(rechazada.replace("\"transactionState\":\"6\"", "\"transactionState\":\"4\"")))
                .isInstanceOf(FirmaInvalidaException.class);
        assertThatThrownBy(() -> payu.verificarEvento(rechazada.replace("\"TX_VALUE\":\"115000.00\"", "\"TX_VALUE\":\"1000.00\"")))
                .isInstanceOf(FirmaInvalidaException.class);
        assertThatThrownBy(() -> payu.verificarEvento("{\"referenceCode\":\"NALU-ABC234-1\"}"))
                .isInstanceOf(FirmaInvalidaException.class);
        assertThatThrownBy(() -> payu.verificarEvento("no es JSON")).isInstanceOf(FirmaInvalidaException.class);
    }

    @Test
    void losEstadosDePayuSeTraducenAlVocabularioDelPuerto() {
        assertThat(PayuPasarela.traducir("4")).isEqualTo("APPROVED");
        assertThat(PayuPasarela.traducir("6")).isEqualTo("DECLINED");
        assertThat(PayuPasarela.traducir("7")).isEqualTo("PENDING");
        assertThat(PayuPasarela.traducir("5")).isEqualTo("ERROR");
        assertThat(PayuPasarela.traducir("104")).isEqualTo("ERROR");
    }
}
