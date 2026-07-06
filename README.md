# PasajeYá — Backend

Microservicio REST para la búsqueda y comparación de precios de vuelos nacionales del Perú, con
alertas de precio por WhatsApp, suscripciones premium, predicción de precios, reportes en
Excel/PDF, panel de administración completo y autenticación segura.

> Proyecto académico — Universidad Tecnológica del Perú (UTP), Ciclo 6, curso **Integrador de
> Sistemas Software**.

---

## 📋 Tabla de contenidos

- [Cuadro de accesos y roles](#-cuadro-de-accesos-y-roles)
- [Tecnologías utilizadas](#-tecnologías-utilizadas)
- [Arquitectura](#-arquitectura)
- [Requisitos previos](#-requisitos-previos)
- [Puesta en marcha (desarrollo local)](#-puesta-en-marcha-desarrollo-local)
- [Endpoints principales](#-endpoints-principales)
- [Panel de administración](#-panel-de-administración)
- [Jobs automáticos](#-jobs-automáticos)
- [Integraciones externas](#-integraciones-externas)
- [Pruebas (Testing)](#-pruebas-testing)
- [Despliegue en Railway](#-despliegue-en-railway)
- [Documentación adicional](#-documentación-adicional)

---

## 🔐 Cuadro de accesos y roles

Los roles y usuarios de prueba se crean en
[`src/main/resources/script.sql`](src/main/resources/script.sql).

### Roles del sistema

| `id_rol` | Rol | Descripción |
|:---:|---|---|
| 1 | `usuario_free` | Máximo **3 alertas** activas · predicción **7 días** |
| 2 | `usuario_premium` | Alertas **ilimitadas** · predicción **15 días** · acceso a reportes Excel/PDF |
| 3 | `admin` | Acceso completo al panel de administración |

### Usuarios de prueba (semilla del script)

Los tres usuarios comparten la misma contraseña de prueba: **`Marco1415@`**
(almacenada como hash BCrypt en el script).

| Usuario / Email | Contraseña | Rol | Plan | Estado |
|---|---|:---:|---|---|
| `admin@pasajeya.com.pe` | `Marco1415@` | `admin` (3) | — | activo · email verificado |
| `enrique.pdg@outlook.com` | `Marco1415@` | `usuario_free` (1) | Free | activo · email verificado |
| `renrique_prada@hotmail.com` | `Marco1415@` | `usuario_premium` (2) | Premium Anual | activo · email verificado |

> El usuario premium tiene además una **suscripción activa** (`Premium Anual`, S/ 120.00,
> vigente 2025-06-09 → 2026-06-09) con su pago asociado, insertada por el script. El script
> también siembra ~12,300 vuelos, ~30,750 tarifas y ~215,250 registros de historial de precios
> a lo largo de 4 meses en 9 rutas domésticas.

### Matriz de acceso por endpoint

| Recurso | Público | `usuario_free` | `usuario_premium` | `admin` |
|---|:---:|:---:|:---:|:---:|
| `POST /api/auth/registro`, `login`, `verificar` | ✅ | ✅ | ✅ | ✅ |
| `GET /api/vuelos`, `/tarifas/{id}`, `/exportar` | ✅ | ✅ | ✅ | ✅ |
| `GET /api/aeropuertos` | ✅ | ✅ | ✅ | ✅ |
| `GET/PUT /api/perfil`, suscripciones | ❌ | ✅ | ✅ | ✅ |
| `GET/POST/PATCH/DELETE /api/alertas` | ❌ | ✅ (máx. 3) | ✅ (ilimitadas) | ❌ (el admin no gestiona alertas propias) |
| `GET /api/alertas/reporte/excel` · `/pdf` | ❌ | ❌ (requiere premium) | ✅ | ✅ |
| `GET/POST/PUT/PATCH/DELETE /api/admin/**` | ❌ | ❌ | ❌ | ✅ |

Todo endpoint privado exige un **JWT válido** (`Authorization: Bearer <token>`). Sin
token, Spring Security responde `401/403` y nunca ejecuta la lógica del controlador. El acceso
al panel admin se verifica con `@PreAuthorize("hasRole('ADMIN')")` a nivel de clase, usando el
claim de rol firmado dentro del JWT — no una bandera editable desde el cliente.

---

## 🛠 Tecnologías utilizadas

| Categoría | Tecnología | Versión |
|---|---|---|
| Lenguaje | Java | 22 |
| Framework | Spring Boot | 3.4.5 |
| Seguridad | Spring Security 6 + JWT (JJWT) | 0.12.6 |
| Persistencia | Spring Data JPA / Hibernate | (Boot 3.4.5) |
| Base de datos | PostgreSQL | 16 |
| Base de datos (pruebas) | H2 (en memoria) | test scope |
| Pool de conexiones (prod) | HikariCP (máx. 10 conexiones) | (Boot 3.4.5) |
| Cifrado de contraseñas | BCryptPasswordEncoder | (Spring Security) |
| Reportes Excel | Apache POI | 5.3.0 |
| Reportes PDF | iText | 5.5.13.4 |
| Correo transaccional | Brevo API REST (vía `RestClient`, sin SMTP) | v3 |
| WhatsApp | Twilio API REST (vía `RestTemplate`, sin SDK) | 2010-04-01 |
| Documentación API | SpringDoc OpenAPI (Swagger UI) | 2.8.8 |
| Utilidades | Apache Commons Lang3 · Google Guava | 3.14.0 · 33.2.1 |
| Boilerplate | Lombok | (managed) |
| Build | Maven (con wrapper `mvnw`) | 3.9.6 |
| Contenedor | Docker (multi-stage: `eclipse-temurin:22-jdk` → `22-jre`) | — |
| **Pruebas** | **JUnit 5 + Mockito + Spring Test** | (Boot 3.4.5) |
| Cobertura | JaCoCo | 0.8.11 |

---

## 🏗 Arquitectura

Arquitectura por capas dentro del paquete `pe.edu.utp.pasajeya.app`:

```
controller/   → Endpoints REST (Auth, Vuelo, Alerta, Aeropuerto, Perfil, Admin)
service/      → Lógica de negocio (interfaces) + impl/ (implementaciones)
repository/   → Spring Data JPA (acceso a datos con PreparedStatement automático)
model/        → Entidades JPA (Usuario, Persona, Rol, Vuelo, Tarifa, Alerta, Suscripcion, ...)
dto/          → Objetos de transferencia (request/response), incluye PaginaDTO<T> genérico
security/     → JwtFilter, JwtUtil, SecurityConfig
```

El script completo de base de datos (esquema + datos de ejemplo) está en
[`src/main/resources/script.sql`](src/main/resources/script.sql).

### Perfiles de configuración

| Archivo | Uso |
|---|---|
| `application.properties` | Común a todos los perfiles (JWT, Brevo, Swagger, jobs, Twilio) |
| `application-dev.properties` | Desarrollo local (BD local, reCAPTCHA de prueba, logs verbosos) |
| `application-prod.properties` | Producción / Railway (todo parametrizado con variables de entorno) |

---

## ✅ Requisitos previos

- **JDK 22**
- **PostgreSQL 16** en ejecución
- Variable de entorno **`JWT_SECRET`** exportada (sin ella el contexto de Spring no arranca,
  ni siquiera para correr los tests — ver [Puesta en marcha](#-puesta-en-marcha-desarrollo-local))
- No es necesario instalar Maven: el proyecto incluye el wrapper `mvnw` / `mvnw.cmd`.

---

## 🚀 Puesta en marcha (desarrollo local)

### 1. Crear la base de datos

En **pgAdmin**, conectado a la BD `postgres`, ejecuta:

```sql
DROP DATABASE IF EXISTS pasajeya;
CREATE DATABASE pasajeya ENCODING = 'UTF8';
```

Luego, conectado ya a la BD `pasajeya`, ejecuta el resto de
[`src/main/resources/script.sql`](src/main/resources/script.sql) (crea todas las
tablas e inserta los datos de ejemplo: usuarios, vuelos, tarifas e historial de precios).

### 2. Configurar credenciales

Ajusta el usuario/contraseña de PostgreSQL en
[`src/main/resources/application-dev.properties`](src/main/resources/application-dev.properties)
si difieren de `postgres` / `1234`.

### 3. Exportar las variables de entorno obligatorias

```powershell
# PowerShell
$env:JWT_SECRET = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
```

```bash
# Linux / Mac
export JWT_SECRET="404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
```

`brevo.api-key` y `brevo.sender-email` tienen valores por defecto vacíos/genéricos en
`application.properties`, así que el backend arranca sin configurarlos — pero el envío real de
correos de verificación fallará hasta que definas `BREVO_API_KEY` (ver
[Integraciones externas](#-integraciones-externas)).

### 4. Ejecutar

```bash
./mvnw spring-boot:run          # Linux / Mac
mvnw.cmd spring-boot:run        # Windows
```

La API queda disponible en `http://localhost:8080` y la documentación Swagger en
`http://localhost:8080/swagger-ui/index.html`.

---

## 🌐 Endpoints principales

### Autenticación (`/api/auth`)

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/registro` | Registro de usuario (envía email de verificación vía Brevo) |
| `GET`  | `/api/auth/verificar` | Verifica el email mediante token (redirige al frontend) |
| `POST` | `/api/auth/login` | Login → devuelve JWT |

### Vuelos (`/api/vuelos`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET`  | `/api/vuelos` | Búsqueda de vuelos por origen/destino/fecha/pasajeros |
| `GET`  | `/api/vuelos/tarifas/{tarifaId}` | Detalle de una tarifa (equipaje, cambios, reembolso) |
| `GET`  | `/api/vuelos/exportar` | Exporta resultados de búsqueda a Excel |

### Aeropuertos (`/api/aeropuertos`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET`  | `/api/aeropuertos` | Lista de aeropuertos disponibles |

### Perfil y suscripciones (`/api/perfil`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET`  | `/api/perfil` | Perfil del usuario autenticado |
| `PUT`  | `/api/perfil` | Actualiza datos personales / contraseña |
| `GET`  | `/api/perfil/suscripcion` | Suscripción vigente (con *lazy expiry* de vencidas) |
| `GET`  | `/api/perfil/suscripciones` | Historial completo de suscripciones |
| `POST` | `/api/perfil/suscripcion` | Procesa un pago (simulado) y activa premium |
| `PATCH`| `/api/perfil/suscripcion/cancelar` | Cancela la suscripción activa |

### Alertas de precio (`/api/alertas`)

| Método | Ruta | Descripción |
|---|---|---|
| `GET`  | `/api/alertas` | Alertas del usuario autenticado |
| `POST` | `/api/alertas` | Crea una alerta de precio (por tarifa o por ruta+fecha) |
| `PATCH`| `/api/alertas/{id}/pausar` · `/reactivar` | Pausa / reactiva una alerta |
| `DELETE`| `/api/alertas/{id}` | Elimina una alerta |
| `GET`  | `/api/alertas/reporte/excel` · `/pdf` | Reporte de alertas (**solo premium**) |

### Panel de administración (`/api/admin`) — requiere rol `admin`

| Método | Ruta | Descripción |
|---|---|---|
| `GET`  | `/api/admin/dashboard` | Métricas generales (usuarios por rol, ingresos, alertas, suscripciones) |
| `GET`  | `/api/admin/dashboard/precios-ruta` | Evolución de precio promedio por ruta (para gráficos) |
| `GET`  | `/api/admin/usuarios` | Lista paginada + búsqueda de usuarios |
| `GET`  | `/api/admin/usuarios/{id}` | Detalle completo de un usuario |
| `POST` | `/api/admin/usuarios` | Crea un usuario (mismo flujo de validación que el registro público) |
| `PUT`  | `/api/admin/usuarios/{id}` | Edita un usuario (password opcional) |
| `PATCH`| `/api/admin/usuarios/{id}/rol` · `/activo` | Cambia rol / activa-desactiva cuenta |
| `GET`  | `/api/admin/usuarios/exportar` | Exporta todos los usuarios a Excel |
| `GET`  | `/api/admin/historial-precios` | Historial de precios paginado + filtros (ruta, fechas, texto) |
| `GET`  | `/api/admin/historial-precios/exportar` | Exporta historial filtrado a Excel |
| `GET`  | `/api/admin/suscripciones` | Suscripciones paginadas + búsqueda |
| `GET`  | `/api/admin/suscripciones/exportar` | Exporta todas las suscripciones a Excel |
| `GET`  | `/api/admin/pagos` | Lista completa de pagos registrados |
| `GET`  | `/api/admin/reportes/resumen` | KPIs ejecutivos (conversión premium, ingreso promedio, ruta top, comparativo mensual) |
| `GET`  | `/api/admin/reportes/exportar-pdf` | Reporte ejecutivo en PDF (dashboard + KPIs) |
| `GET`  | `/api/admin/vuelos-job/estado` | Estado real del job de captura de precios (última/próxima ejecución, totales) |
| `POST` | `/api/admin/vuelos-job/ejecutar-ahora` | Dispara manualmente el job real de captura de precios |

---

## 🖥 Panel de administración

El panel admin (consumido por el frontend Angular) se organiza en 7 secciones, todas protegidas
por `@PreAuthorize("hasRole('ADMIN')")`:

1. **Dashboard** — 6 gráficos (usuarios por rol, activos/inactivos, suscripciones por estado,
   ingresos mensuales, evolución de precio por ruta, alertas por aerolínea).
2. **Usuarios** — CRUD completo (listar con paginación server-side + búsqueda, crear, editar,
   activar/desactivar, cambiar rol) reutilizando las mismas reglas de validación y normalización
   del registro público (`AuthServiceImpl`).
3. **Historial de precios** — tabla paginada con filtros por ruta/fecha/texto + export a Excel.
4. **Suscripciones y pagos** — tabla paginada + búsqueda, y vista de todos los pagos.
5. **Reportes** — KPIs ejecutivos y comparativo mes actual vs. anterior.
6. **Exportación** — descarga de usuarios (Excel), suscripciones (Excel) y reporte ejecutivo
   (PDF) en un solo lugar.
7. **Job de precios** — estado real del job automático de captura de precios (última ejecución,
   próxima estimada, totales de tarifas/vuelos/historial) con botón para dispararlo manualmente.

Toda la paginación server-side usa el genérico
[`PaginaDTO<T>`](src/main/java/pe/edu/utp/pasajeya/app/dto/PaginaDTO.java), construido a partir
de `Page<T>` de Spring Data (`pagina`, `tamaño`, `totalPaginas`, `totalElementos`).

---

## ⏱ Jobs automáticos

| Job | Frecuencia | Qué hace |
|---|---|---|
| `HistorialPrecioJobService` | Cada 6h (`app.historial.capture-rate-ms`) | Recorre todas las tarifas vigentes, registra su precio actual en `historial_precio` y evalúa las alertas activas (dispara WhatsApp si corresponde) |
| `SuscripcionAutoRenovacionJobService` | Cada 1h (`app.autorenovacion.rate-ms`) | Renueva automáticamente las suscripciones con `auto_renovar = true` próximas a vencer |

El job de historial expone su estado (última ejecución, tasa configurada) a través de
`AdminVueloJobService`, consumido por la pestaña **Job de precios** del panel admin.

---

## 🔌 Integraciones externas

### Brevo (correo transaccional)

El envío de correos (verificación de cuenta) usa la **API REST de Brevo**
(`https://api.brevo.com/v3/smtp/email`) a través de `RestClient`, sin SMTP ni starter de mail:

```properties
brevo.api-key=${BREVO_API_KEY:}
brevo.sender-email=${BREVO_SENDER_EMAIL:no-reply@pasajeya.com}
```

Requiere una cuenta en [Brevo](https://app.brevo.com) con un remitente verificado. En
producción (`application-prod.properties`) ambas variables son obligatorias, sin valor por
defecto.

### Twilio (WhatsApp)

Las notificaciones de alertas de precio se envían por WhatsApp usando la **API REST de
Twilio**, también sin SDK (`RestTemplate` + Basic Auth):

```properties
app.twilio.enabled=${TWILIO_ENABLED:false}
app.twilio.account-sid=${TWILIO_ACCOUNT_SID:}
app.twilio.auth-token=${TWILIO_AUTH_TOKEN:}
```

Con `app.twilio.enabled=false` (default), el mensaje solo se loguea (`[TWILIO DEMO]`) sin
llamar a la API — útil para demos sin cuenta de Twilio configurada.

### reCAPTCHA v2

Protege el registro público contra bots (`app.recaptcha.enabled`, `secret`, `site-key`).

---

## 🧪 Pruebas (Testing)

El proyecto incluye una batería de **194 pruebas automatizadas** distribuidas en 32 clases,
siguiendo la pirámide de testing (unitarias → integración → E2E) y el patrón
**AAA (Arrange–Act–Assert)**.

| Tipo de prueba | Herramienta | Alcance | Ejemplos |
|---|---|---|---|
| **Unitarias (servicios)** | JUnit 5 + Mockito | Lógica de negocio aislada con mocks | `AuthServiceTest`, `VueloServiceTest`, `AlertaServiceTest`, `SuscripcionServiceTest`, `EmailServiceTest` |
| **Unitarias (módulo Admin)** | JUnit 5 + Mockito | CRUD, paginación, dashboard, reportes, job de precios | `AdminUsuarioServiceTest`, `AdminDashboardServiceTest`, `AdminHistorialPrecioServiceTest`, `AdminSuscripcionServiceTest`, `AdminVueloJobServiceTest` |
| **Unitarias (jobs)** | JUnit 5 + Mockito | Captura de precios, auto-renovación | `HistorialPrecioJobServiceTest`, `SuscripcionAutoRenovacionJobServiceTest` |
| **Integración (repositorios)** | `@DataJpaTest` + H2 | Consultas JPA contra BD en memoria | `VueloRepositoryTest`, `AlertaRepositoryTest`, `AeropuertoRepositoryTest` |
| **Web (controladores)** | `@WebMvcTest` + MockMvc | Endpoints REST con `@MockitoBean` | `AuthControllerTest`, `AlertaControllerTest`, `PerfilControllerTest`, `VueloControllerTest` |
| **Seguridad (OWASP)** | JUnit 5 + Mockito + Spring Security Test | A01, A03, A07 del OWASP Top 10 + control de acceso admin | `A01BrokenAccessControlTest`, `A03SqlInjectionTest`, `A07AutenticacionFallidaTest`, `AdminAccessControlTest` |
| **Contexto** | `@SpringBootTest` | Arranque completo del contexto Spring | `PasajeYaApplicationTests` |

### Ejecutar las pruebas

> **Requiere `JWT_SECRET` exportado en el entorno** (ver [Puesta en marcha](#-puesta-en-marcha-desarrollo-local)) — sin ella, ni siquiera `PasajeYaApplicationTests` levanta el contexto.

```bash
# Todas las pruebas + reporte de cobertura JaCoCo
mvnw.cmd clean test

# Solo las pruebas de seguridad (perfil dedicado)
mvnw.cmd test -P seguridad

# Empaquetar (ejecuta todas las pruebas; falla si alguna no pasa)
mvnw.cmd clean install
```

El reporte de cobertura **JaCoCo** se genera en `target/site/jacoco/index.html`.

### Pruebas de seguridad — OWASP Top 10

Las clases del paquete `security/` verifican, con código ejecutable, que PasajeYá corrige
las vulnerabilidades del taller (Sistema de Matrícula vulnerable):

- **A01 – Control de Acceso Roto:** todo endpoint privado exige JWT; el email se obtiene
  del token (no de la URL), evitando IDOR; los reportes verifican rol premium; el panel admin
  completo exige rol `admin` verificado por claim firmado (`AdminAccessControlTest`).
- **A03 – Inyección SQL:** Spring Data JPA usa `PreparedStatement`; un payload SQL en el
  email no produce resultados.
- **A07 – Fallas de Autenticación:** contraseñas con **BCrypt** (salt aleatorio, no MD5),
  verificación de email obligatoria y bloqueo de cuentas inactivas.

---

## 🚢 Despliegue en Railway

El proyecto incluye un [`Dockerfile`](Dockerfile) multi-stage (`eclipse-temurin:22-jdk` para
compilar con `./mvnw`, `eclipse-temurin:22-jre` para ejecutar el jar), listo para que Railway lo
detecte automáticamente sin configuración adicional de build.

### Variables de entorno requeridas en Railway

| Variable | Descripción |
|---|---|
| `JWT_SECRET` | Secreto para firmar los JWT (puede compartirse con otros proyectos propios) |
| `DB_HOST`, `DB_PORT`, `DB_NAME`, `DB_USERNAME`, `DB_PASSWORD` | Conexión a PostgreSQL (Railway las genera si agregas su plugin de Postgres) |
| `APP_URL` | URL pública que Railway asigna a este servicio (se usa para construir enlaces de verificación) |
| `FRONTEND_URL` | URL del frontend desplegado |
| `RECAPTCHA_SECRET`, `RECAPTCHA_SITE_KEY` | Claves reales de Google reCAPTCHA v2 |
| `BREVO_API_KEY`, `BREVO_SENDER_EMAIL` | Credenciales de la cuenta Brevo para envío de correo |
| `TWILIO_ENABLED`, `TWILIO_ACCOUNT_SID`, `TWILIO_AUTH_TOKEN` | Opcionales, solo si se activa el envío real de WhatsApp |

### Pasos

1. Crear el proyecto en Railway y agregar un plugin de PostgreSQL.
2. Conectar este repositorio (Railway detecta el `Dockerfile` automáticamente).
3. Configurar todas las variables de la tabla anterior en el servicio del backend.
4. Ejecutar manualmente `script.sql` contra la base de datos de Railway (no se aplica solo —
   es un script de creación completo, no una migración automática).
5. Desplegar, copiar la URL pública generada y actualizar `APP_URL` con ese valor si aún no se
   conocía de antemano.

---

## 📄 Documentación adicional

- **Reporte de hallazgos de seguridad:** [`src/test/resources/REPORTE_HALLAZGOS_PASAJEYÁ.html`](src/test/resources/REPORTE_HALLAZGOS_PASAJEYÁ.html)
- **Documento formal (Word):** `recursos/semana13/PasajeYa_APF4_seguridad.docx`
- **Swagger UI:** `http://localhost:8080/swagger-ui/index.html` (con la app en ejecución)

---

<p align="center"><sub>PasajeYá · Proyecto académico UTP 2026 · Curso Integrador de Sistemas Software</sub></p>
