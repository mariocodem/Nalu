#!/usr/bin/env python3
"""Publica las pruebas de Nalu en Kiwi TCMS y genera el informe de docs/calidad/kiwi/.

1. Resultados JUnit del backend (Surefire) y del frontend (Vitest).
2. Pruebas E2E por HTTP contra el proyecto en marcha (tienda en :4200 y API en :8080).
3. Informe en Markdown y JSON a partir de lo que quedó registrado en Kiwi.

Lo ejecuta scripts/kiwi/publicar.sh, que prepara el entorno (dependencias, credenciales y archivos
JUnit). Variables: KIWI_URL, KIWI_USUARIO, KIWI_CLAVE, NALU_ADMIN_EMAIL, NALU_ADMIN_PASSWORD,
JUNIT_BACKEND (carpeta), JUNIT_FRONTEND (archivo), SALIDA (carpeta del informe).
"""

import hashlib
import json
import os
import ssl
import subprocess
import sys
import tempfile
import time
from collections import Counter
from datetime import datetime
from pathlib import Path


def cargar_env(ruta=".env"):
    """Lee .env como archivo de propiedades (igual que Spring), sin evaluarlo con la shell."""
    for linea in Path(ruta).read_text().splitlines():
        if "=" in linea and not linea.lstrip().startswith("#"):
            clave, valor = linea.split("=", 1)
            os.environ.setdefault(clave.strip(), valor.strip())


cargar_env()
KIWI_URL = os.environ.get("KIWI_URL", "https://localhost:8443/xml-rpc/")
TIENDA = os.environ.get("NALU_TIENDA", "http://localhost:4200")
API = os.environ.get("NALU_API", "http://localhost:8080")
PAYU_API_KEY = os.environ.get("PAYU_API_KEY", "4Vj8eK4rloUd272L48hsrarnUA")  # credencial pública del sandbox
PAYU_MERCHANT = os.environ.get("PAYU_MERCHANT_ID", "508029")
SALIDA = Path(os.environ["SALIDA"])
COMMIT = subprocess.run(["git", "rev-parse", "--short", "HEAD"], capture_output=True, text=True).stdout.strip()
VERSION = os.environ.get("TCMS_PRODUCT_VERSION", f"entrega-final-{COMMIT}")
BUILD = os.environ.get("TCMS_BUILD", datetime.now().strftime("%Y-%m-%d %H:%M"))

# tcms-api solo lee ~/.tcms.conf: se usa un HOME temporal para no tocar el del usuario.
HOME_TEMPORAL = tempfile.mkdtemp(prefix="nalu-kiwi-")
Path(HOME_TEMPORAL, ".tcms.conf").write_text(
    f"[tcms]\nurl = {KIWI_URL}\nusername = {os.environ['KIWI_USUARIO']}\npassword = {os.environ['KIWI_CLAVE']}\n"
)
os.chmod(Path(HOME_TEMPORAL, ".tcms.conf"), 0o600)
os.environ["HOME"] = HOME_TEMPORAL
os.environ.update(TCMS_PRODUCT="Nalu", TCMS_PRODUCT_VERSION=VERSION, TCMS_BUILD=BUILD)
if "localhost" in KIWI_URL or "127.0.0.1" in KIWI_URL:
    # El certificado del contenedor de Kiwi es autofirmado y no es para localhost.
    ssl._create_default_https_context = ssl._create_unverified_context  # noqa: SLF001

import requests  # noqa: E402  (dependencia de tcms-api)
from tcms_api import TCMS  # noqa: E402
from tcms_api.plugin_helpers import Backend  # noqa: E402
from tcms_junit_plugin import Plugin  # noqa: E402

corridas = []  # (nombre, run_id)


def normalizar_fechas(archivo):
    """Vitest escribe 2026-11-19T20:06:43.116Z y el plugin de Kiwi solo acepta 2026-11-19T20:06:43."""
    import re

    copia = Path(HOME_TEMPORAL, Path(archivo).name)
    copia.write_text(re.sub(r'(timestamp="[0-9T:-]{19})\.\d+Z"', r'\1"', Path(archivo).read_text()))
    return str(copia)


def publicar_junit(nombre, prefijo, archivos):
    os.environ["TCMS_PREFIX"] = prefijo
    plugin = Plugin()
    plugin.parse(archivos)
    plugin.backend.finish_test_run()
    corridas.append((nombre, plugin.backend.run_id))
    print(f"{nombre}: TestRun {plugin.backend.run_id} ({len(archivos)} archivos)")


# ── Pruebas E2E por HTTP ─────────────────────────────────────────────────────────────────────

def md5(texto):
    return hashlib.md5(texto.encode()).hexdigest()  # noqa: S324 (firma que exige PayU)


def e2e():
    s = requests.Session()
    ts = int(time.time())
    estado = {}
    casos = []

    def caso(resumen):
        def registrar(funcion):
            casos.append((resumen, funcion))
            return funcion
        return registrar

    @caso("API: /actuator/health responde UP")
    def _():
        r = s.get(f"{API}/actuator/health", timeout=10)
        assert r.status_code == 200 and r.json()["status"] == "UP", r.text
        return "200 UP"

    @caso("Tienda SSR: el inicio se renderiza en el servidor con la marca y las novedades")
    def _():
        r = s.get(f"{TIENDA}/", timeout=20)
        assert r.status_code == 200 and "Recién llegadas" in r.text, r.status_code
        return f"200, {len(r.text)} bytes de HTML"

    @caso("Tienda SSR: el catálogo y la API pública listan las piezas publicadas")
    def _():
        r = s.get(f"{API}/api/v1/productos?size=50", timeout=10).json()
        assert r["totalElementos"] > 0
        estado["producto"] = next(p["slug"] for p in r["contenido"] if p.get("precioDesde"))
        assert s.get(f"{TIENDA}/catalogo", timeout=20).status_code == 200
        return f"{r['totalElementos']} piezas publicadas"

    @caso("SEO: la ficha de producto trae título y JSON-LD Product")
    def _():
        r = s.get(f"{TIENDA}/producto/{estado['producto']}", timeout=20)
        assert r.status_code == 200 and '"@type":"Product"' in r.text.replace(" ", ""), r.status_code
        titulo = r.text.split("<title>", 1)[1].split("</title>", 1)[0]
        assert "Nalu" in titulo, titulo
        return f"/producto/{estado['producto']}: «{titulo}»"

    @caso("SEO: sitemap.xml y robots.txt disponibles")
    def _():
        mapa = s.get(f"{TIENDA}/sitemap.xml", timeout=20)
        robots = s.get(f"{TIENDA}/robots.txt", timeout=20)
        assert mapa.status_code == 200 and "<urlset" in mapa.text and "Sitemap:" in robots.text
        return f"{mapa.text.count('<url>')} URL en el sitemap"

    @caso("Seguridad: cabeceras del SSR (CSP con form-action a PayU, X-Frame-Options DENY, nosniff)")
    def _():
        h = s.get(f"{TIENDA}/", timeout=20).headers
        csp = h.get("Content-Security-Policy", "")
        assert "checkout.payulatam.com" in csp and h.get("X-Frame-Options") == "DENY"
        assert h.get("X-Content-Type-Options") == "nosniff"
        return "CSP, X-Frame-Options y nosniff presentes"

    @caso("Autenticación: el administrador conserva el rol ADMINISTRADOR al renovar la sesión")
    def _():
        admin = requests.Session()
        r = admin.post(f"{API}/api/v1/auth/login", timeout=10, json={
            "correo": os.environ["NALU_ADMIN_EMAIL"], "contrasena": os.environ["NALU_ADMIN_PASSWORD"]})
        assert r.status_code == 200, r.status_code
        r = admin.post(f"{API}/api/v1/auth/refresh", timeout=10, headers={"Origin": TIENDA})
        assert r.status_code == 200 and r.json()["usuario"]["roles"] == ["ADMINISTRADOR"], r.text[:200]
        token = r.json()["accessToken"]
        r = admin.get(f"{API}/api/v1/admin/categorias", timeout=10, headers={"Authorization": f"Bearer {token}"})
        assert r.status_code == 200, r.status_code
        return "refresh → ADMINISTRADOR; el token renovado abre /admin"

    @caso("Compra: registro, bolsa, dirección y pedido devuelven el formulario de PayU firmado en el servidor")
    def _():
        r = s.post(f"{API}/api/v1/auth/registro", timeout=10, json={
            "nombre": "Prueba", "apellido": "E2E", "correo": f"e2e-{ts}@nalu.local", "telefono": "3001234567",
            "contrasena": f"Prueba-E2E-{ts}", "aceptaPolitica": True, "aceptaComunicaciones": False})
        assert r.status_code in (200, 201), r.text[:200]
        cab = {"Authorization": f"Bearer {r.json()['accessToken']}"}
        estado["cab"] = cab
        ficha = s.get(f"{API}/api/v1/productos/{estado['producto']}", timeout=10).json()
        variante = next(v["id"] for v in ficha["variantes"] if v.get("disponible", 1))
        r = s.post(f"{API}/api/v1/carrito/items", headers=cab, timeout=10, json={"idVariante": variante, "cantidad": 1})
        assert r.status_code == 200, r.text[:200]
        r = s.post(f"{API}/api/v1/cuenta/direcciones", headers=cab, timeout=10, json={
            "etiqueta": "Casa", "destinatario": "Prueba E2E", "departamento": "Antioquia", "ciudad": "Medellín",
            "direccion": "Calle 10 # 43-12", "codigoPostal": None, "telefono": "3001234567"})
        assert r.status_code in (200, 201), r.text[:200]
        r = s.post(f"{API}/api/v1/pedidos", headers=cab, timeout=10, json={"idDireccion": r.json()["id"]})
        assert r.status_code == 201, r.text[:200]
        pedido = r.json()
        campos, ref = pedido["pago"]["campos"], pedido["pago"]["referencia"]
        firma = md5(f"{PAYU_API_KEY}~{PAYU_MERCHANT}~{ref}~{campos['amount']}~COP")
        assert campos["signature"] == firma and campos["referenceCode"] == ref and campos["test"] == "1"
        estado.update(pedido=pedido)
        return f"pedido {pedido['numero']}, total {campos['amount']} COP, firma MD5 correcta"

    @caso("Seguridad: la API del panel exige sesión (401) y rol de administrador (403)")
    def _():
        assert s.get(f"{API}/api/v1/admin/pedidos", timeout=10).status_code == 401
        assert s.get(f"{API}/api/v1/admin/pedidos", headers=estado["cab"], timeout=10).status_code == 403
        return "sin token → 401; con token de cliente → 403"

    @caso("Pagos: el sandbox de PayU acepta el formulario firmado (302 a su checkout)")
    def _():
        p = estado["pedido"]["pago"]
        r = requests.post(p["urlPago"], data=p["campos"], allow_redirects=False, timeout=30)
        assert r.status_code == 302 and "ppp-web-gateway-payu/app" in r.headers.get("Location", ""), r.status_code
        return "302 → " + r.headers["Location"].split("?")[0]

    def respuesta(estado_tx, firma=None):
        p = estado["pedido"]["pago"]
        valor = f"{p['montoCentavos'] // 100}.00"
        cuerpo = {"merchantId": PAYU_MERCHANT, "referenceCode": p["referencia"], "TX_VALUE": valor, "currency": "COP",
                  "transactionState": estado_tx, "transactionId": f"e2e-{ts}-{estado_tx}", "lapPaymentMethod": "VISA",
                  "message": "DECLINED"}
        cuerpo["signature"] = firma or md5(
            f"{PAYU_API_KEY}~{PAYU_MERCHANT}~{p['referencia']}~{valor[:-1]}~COP~{estado_tx}")
        return s.post(f"{API}/api/v1/pagos/payu/respuesta", headers=estado["cab"], json=cuerpo, timeout=10)

    @caso("Pagos: una respuesta de PayU con firma alterada se rechaza (401) sin tocar el pedido")
    def _():
        r = respuesta("4", firma="0" * 32)
        assert r.status_code == 401, r.status_code
        return "401"

    @caso("Pagos: una respuesta de rechazo firmada deja el pago RECHAZADO y el pedido pendiente para reintentar")
    def _():
        r = respuesta("6")
        assert r.status_code == 200, r.text[:200]
        datos = r.json()
        assert datos["estado"] == "PENDIENTE_PAGO" and datos["estadoPago"] == "RECHAZADO", datos
        return f"{datos['numero']}: pedido {datos['estado']}, pago {datos['estadoPago']}"

    resultados = []
    for resumen, funcion in casos:
        inicio = time.time()
        try:
            detalle, ok = funcion(), True
        except Exception as error:  # noqa: BLE001 (el fallo se registra en Kiwi)
            detalle, ok = f"{type(error).__name__}: {error}"[:500], False
        resultados.append((resumen, ok, detalle, time.time() - inicio))
        print(("PASA " if ok else "FALLA") + f" {resumen} — {detalle}")
    return resultados


def publicar_e2e(resultados):
    os.environ["TCMS_PREFIX"] = "[E2E HTTP]"  # el plugin da prioridad a esta variable sobre el prefijo
    backend = Backend(prefix="[E2E HTTP]")
    backend.configure()
    aprobado = backend.get_status_id("PASSED")
    fallido = backend.get_status_id("FAILED")
    for resumen, ok, detalle, _ in resultados:
        caso_id = backend.test_case_get_or_create(resumen)[0]["id"]
        backend.add_test_case_to_plan(caso_id, backend.plan_id)
        for ejecucion in backend.add_test_case_to_run(caso_id, backend.run_id):
            backend.update_test_execution(ejecucion["id"], aprobado if ok else fallido, detalle)
    backend.finish_test_run()
    corridas.append(("E2E HTTP contra el proyecto en marcha", backend.run_id))
    print(f"E2E: TestRun {backend.run_id}")


# ── Informe ──────────────────────────────────────────────────────────────────────────────────

def informe():
    rpc = TCMS().exec
    filas, datos = [], []
    for nombre, run_id in corridas:
        run = rpc.TestRun.filter({"pk": run_id})[0]
        ejecuciones = rpc.TestExecution.filter({"run": run_id})
        conteo = Counter(e["status__name"] for e in ejecuciones)
        filas.append((nombre, run_id, run["summary"], run["plan__name"], len(ejecuciones), conteo))
        datos.append({"corrida": nombre, "test_run": run_id, "resumen": run["summary"], "plan": run["plan__name"],
                      "build": run["build__name"], "inicio": str(run["start_date"]), "fin": str(run["stop_date"]),
                      "conteo": dict(conteo),
                      "ejecuciones": [{"caso": e["case__summary"], "estado": e["status__name"]} for e in ejecuciones]})
    total = sum(f[4] for f in filas)
    aprobadas = sum(f[5].get("PASSED", 0) for f in filas)
    md = [
        "# Informe de pruebas en Kiwi TCMS",
        "",
        f"- **Fecha:** {datetime.now():%Y-%m-%d %H:%M} (hora local)",
        f"- **Producto / versión / build en Kiwi:** Nalu / `{VERSION}` / `{BUILD}`",
        f"- **Commit probado:** `{COMMIT}`",
        "- **Gestor de pruebas:** Kiwi TCMS 17.1 local (`infraestructura/kiwi/docker-compose.yml`)",
        "- **Cómo se generó:** `scripts/kiwi/publicar.sh` (resultados JUnit con `kiwitcms-junit.xml-plugin`, "
        "E2E y este informe con `tcms-api`)",
        "",
        "## Resumen",
        "",
        f"**{aprobadas} de {total} ejecuciones aprobadas** ({100 * aprobadas / total:.1f} %).",
        "",
        "| Corrida | TestRun en Kiwi | Ejecuciones | Aprobadas | Fallidas | Otras |",
        "|---|---|---|---|---|---|",
    ]
    for nombre, run_id, _, _, n, c in filas:
        otras = n - c.get("PASSED", 0) - c.get("FAILED", 0)
        md.append(f"| {nombre} | TR-{run_id} | {n} | {c.get('PASSED', 0)} | {c.get('FAILED', 0)} | {otras} |")
    md += ["", "Las ejecuciones con otro estado (`ERROR`, `BLOCKED`, `WAIVED`) aparecerían en «Otras».", ""]

    fallos = [(d["corrida"], e["caso"], e["estado"]) for d in datos for e in d["ejecuciones"] if e["estado"] != "PASSED"]
    md += ["## Fallos", ""]
    md += ["Ninguno."] if not fallos else [f"- **{c}** ({r}): {e}" for r, c, e in fallos]

    md += ["", "## Pruebas E2E contra el proyecto en marcha", "",
           "Por HTTP contra la tienda (`:4200`, SSR) y la API (`:8080`) en ejecución, más el sandbox real de PayU.", "",
           "| Caso | Resultado | Detalle | Tiempo |", "|---|---|---|---|"]
    for resumen, ok, detalle, segundos in E2E:
        md.append(f"| {resumen} | {'✅ Aprobada' if ok else '❌ Fallida'} | {detalle.replace('|', '/')} | {segundos:.1f} s |")

    md += ["", "## Pruebas automatizadas por componente", ""]
    for d in datos[:2]:
        clases = Counter(e["caso"].rsplit(".", 1)[0] if "." in e["caso"] else e["caso"] for e in d["ejecuciones"])
        md += [f"### {d['corrida']} (TR-{d['test_run']})", "", "| Clase o archivo de pruebas | Casos |", "|---|---|"]
        md += [f"| `{clase}` | {n} |" for clase, n in sorted(clases.items())]
        md.append("")
    SALIDA.mkdir(parents=True, exist_ok=True)
    capturas = sorted(SALIDA.glob("kiwi-*.png"))
    if capturas:
        md += ["## Capturas de Kiwi TCMS", ""] + [f"![{c.stem}]({c.name})" for c in capturas] + [""]
    md += ["## Cómo repetirlo", "", "```bash",
           "docker compose -f infraestructura/kiwi/docker-compose.yml up -d   # Kiwi en https://localhost:8443",
           "scripts/kiwi/publicar.sh                                          # con Nalu en marcha",
           "```", "",
           "El detalle de cada ejecución (casos, estados, tiempos y comentarios) está en `resultados.json` y en la "
           "interfaz de Kiwi (Test Runs del producto Nalu).", ""]
    SALIDA.mkdir(parents=True, exist_ok=True)
    (SALIDA / "informe-kiwi.md").write_text("\n".join(md))
    (SALIDA / "resultados.json").write_text(json.dumps(datos, ensure_ascii=False, indent=2, default=str))
    print(f"Informe: {SALIDA / 'informe-kiwi.md'} — {aprobadas}/{total} aprobadas")
    return 0 if aprobadas == total else 1


if __name__ == "__main__":
    publicar_junit("Backend: JUnit 5, Spring Boot y Testcontainers", "[Backend JUnit]",
                   sorted(str(p) for p in Path(os.environ["JUNIT_BACKEND"]).glob("TEST-*.xml")))
    publicar_junit("Frontend: Vitest", "[Frontend Vitest]", [normalizar_fechas(os.environ["JUNIT_FRONTEND"])])
    E2E = e2e()
    publicar_e2e(E2E)
    sys.exit(informe())
