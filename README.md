# PasajeYá — Backend

Microservicio REST para la búsqueda y comparación de precios de vuelos nacionales del Perú, con alertas de precio, generación de reportes y autenticación segura.

> Proyecto académico — Universidad Tecnológica del Perú (UTP), Ciclo 6, curso **Integrador de Sistemas Software**.

---

## 📋 Tabla de contenidos

- [Cuadro de accesos y roles](#-cuadro-de-accesos-y-roles)
- [Tecnologías utilizadas](#-tecnologías-utilizadas)
- [Arquitectura](#-arquitectura)
- [Requisitos previos](#-requisitos-previos)
- [Puesta en marcha](#-puesta-en-marcha)
- [Endpoints principales](#-endpoints-principales)
- [Pruebas (Testing)](#-pruebas-testing)
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
| 3 | `admin` | Acceso completo al sistema |

### Usuarios de prueba (semilla del script)

Los tres usuarios comparten la misma contraseña de prueba (hash BCrypt en el script).

| Usuario / Email | Rol | Plan | Estado |
|---|:---:|---|---|
| `admin@pasajeya.com.pe` | `admin` (3) | — | activo · email verificado |
| `enrique.pdg@gmail.com` | `usuario_free` (1) | Free | activo · email verificado |
| `renrique_prada@hotmail.com` | `usuario_premium` (2) | Premium Anual | activo · email verificado |

> El usuario premium tiene además una **suscripción activa** (`Premium Anual`, S/ 120.00,
> vigente 2025-06-09 → 2026-06-09) con su pago asociado, insertada por el script.

### Matriz de acceso por endpoint

| Recurso | Público | `usuario_free` | `usuario_premium` | `admin` |
|---|:---:|:---:|:---:|:---:|
| `POST /api/auth/registro`, `login`, `verificar` | ✅ | ✅ | ✅ | ✅ |
| `GET /api/vuelos`, `/tarifas/{id}`, `/exportar` | ✅ | ✅ | ✅ | ✅ |
| `GET /api/aeropuertos` | ✅ | ✅ | ✅ | ✅ |
| `GET/PUT /api/perfil` | ❌ | ✅ | ✅ | ✅ |
| `GET/POST/DELETE /api/alertas` | ❌ | ✅ (máx. 3) | ✅ (ilimitadas) | ✅ |
| `GET /api/alertas/reporte/excel` · `/pdf` | ❌ | ❌ (requiere premium) | ✅ | ✅ |

Todo endpoint privado exige un **JWT válido** (`Authorization: Bearer <token>`). Sin
token, Spring Security responde `401/403` y nunca ejecuta la lógica del controlador.

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
| Cifrado de contraseñas | BCryptPasswordEncoder | (Spring Security) |
| Reportes Excel | Apache POI | 5.3.0 |
| Reportes PDF | iText | 5.5.13.4 |
| Correo | Spring Boot Starter Mail | (Boot 3.4.5) |
| Documentación API | SpringDoc OpenAPI (Swagger UI) | 2.8.8 |
| Utilidades | Apache Commons Lang3 · Google Guava | 3.14.0 · 33.2.1 |
| Boilerplate | Lombok | (managed) |
| Build | Maven (con wrapper `mvnw`) | 3.9.6 |
| **Pruebas** | **JUnit 5 + Mockito + Spring Test** | (Boot 3.4.5) |
| Cobertura | JaCoCo | 0.8.11 |

---

## 🏗 Arquitectura

Arquitectura por capas dentro del paquete `pe.edu.utp.pasajeya.app`:

```
controller/   → Endpoints REST (Auth, Vuelo, Alerta, Aeropuerto, Perfil)
service/      → Lógica de negocio (interfaces) + impl/ (implementaciones)
repository/   → Spring Data JPA (acceso a datos con PreparedStatement automático)
model/        → Entidades JPA (Usuario, Persona, Rol, Vuelo, Tarifa, Alerta, ...)
dto/          → Objetos de transferencia (request/response)
security/     → JwtFilter, JwtUtil, SecurityConfig
config/       → CORS, OpenAPI, manejo global de excepciones
```

El script completo de base de datos (esquema + datos de ejemplo) está en
[`src/main/resources/script.sql`](src/main/resources/script.sql).

---

## ✅ Requisitos previos

- **JDK 22**
- **PostgreSQL 16** en ejecución
- No es necesario instalar Maven: el proyecto incluye el wrapper `mvnw` / `mvnw.cmd`.

---

## 🚀 Puesta en marcha

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
[`src/main/resources/application-dev.properties`](src/main/resources/application-dev.properties).

### 3. Ejecutar

```bash
./mvnw spring-boot:run          # Linux / Mac
mvnw.cmd spring-boot:run        # Windows
```

La API queda disponible en `http://localhost:8080` y la documentación Swagger en
`http://localhost:8080/swagger-ui.html`.

---

## 🌐 Endpoints principales

| Método | Ruta | Descripción |
|---|---|---|
| `POST` | `/api/auth/registro` | Registro de usuario (envía email de verificación) |
| `POST` | `/api/auth/login` | Login → devuelve JWT |
| `GET`  | `/api/auth/verificar` | Verifica el email mediante token |
| `GET`  | `/api/vuelos` | Búsqueda de vuelos por origen/destino/fecha |
| `GET`  | `/api/vuelos/tarifas/{id}` | Detalle de una tarifa |
| `GET`  | `/api/vuelos/exportar` | Exporta resultados a Excel |
| `GET`  | `/api/aeropuertos` | Lista de aeropuertos |
| `GET`  | `/api/perfil` | Perfil del usuario autenticado |
| `PUT`  | `/api/perfil` | Actualiza el perfil |
| `GET`  | `/api/alertas` | Alertas del usuario autenticado |
| `POST` | `/api/alertas` | Crea una alerta de precio |
| `PATCH`| `/api/alertas/{id}/pausar` · `/reactivar` | Pausa / reactiva una alerta |
| `DELETE`| `/api/alertas/{id}` | Elimina una alerta |
| `GET`  | `/api/alertas/reporte/excel` · `/pdf` | Reporte de alertas (**solo premium**) |

---

## 🧪 Pruebas (Testing)

El proyecto incluye una batería completa de **125 pruebas automatizadas** distribuidas en
24 clases, siguiendo la pirámide de testing (unitarias → integración → E2E) y el patrón
**AAA (Arrange–Act–Assert)**.

| Tipo de prueba | Herramienta | Alcance | Ejemplos |
|---|---|---|---|
| **Unitarias (servicios)** | JUnit 5 + Mockito | Lógica de negocio aislada con mocks | `AuthServiceTest`, `VueloServiceTest`, `AlertaServiceTest` |
| **Integración (repositorios)** | `@DataJpaTest` + H2 | Consultas JPA contra BD en memoria | `VueloRepositoryTest`, `AlertaRepositoryTest`, `AeropuertoRepositoryTest` |
| **Web (controladores)** | `@WebMvcTest` + MockMvc | Endpoints REST con `@MockitoBean` | `AuthControllerTest`, `AlertaControllerTest`, `PerfilControllerTest` |
| **Seguridad (OWASP)** | JUnit 5 + Mockito + Spring Security Test | A01, A03, A07 del OWASP Top 10 | `A01BrokenAccessControlTest`, `A03SqlInjectionTest`, `A07AutenticacionFallidaTest` |
| **Contexto** | `@SpringBootTest` | Arranque del contexto | `PasajeYaApplicationTests` |

### Ejecutar las pruebas

```bash
# Todas las pruebas + reporte de cobertura JaCoCo
mvnw.cmd clean test

# Solo las pruebas de seguridad (perfil dedicado)
mvnw.cmd test -P seguridad

# Empaquetar (ejecuta todas las pruebas; falla si alguna no pasa)
mvnw.cmd clean install
```

El reporte de cobertura **JaCoCo** se genera en `target/site/jacoco/index.html`
(cobertura aproximada **~86 %**).

### Pruebas de seguridad — OWASP Top 10

Las clases del paquete `security/` verifican, con código ejecutable, que PasajeYá corrige
las vulnerabilidades del taller (Sistema de Matrícula vulnerable):

- **A01 – Control de Acceso Roto:** todo endpoint privado exige JWT; el email se obtiene
  del token (no de la URL), evitando IDOR; los reportes verifican rol premium.
- **A03 – Inyección SQL:** Spring Data JPA usa `PreparedStatement`; un payload SQL en el
  email no produce resultados.
- **A07 – Fallas de Autenticación:** contraseñas con **BCrypt** (salt aleatorio, no MD5),
  verificación de email obligatoria y bloqueo de cuentas inactivas.

---

## 📄 Documentación adicional

- **Reporte de hallazgos de seguridad:** [`src/test/resources/REPORTE_HALLAZGOS_PASAJEYÁ.html`](src/test/resources/REPORTE_HALLAZGOS_PASAJEYÁ.html)
- **Documento formal (Word):** `recursos/semana13/PasajeYa_APF4_seguridad.docx`
- **Swagger UI:** `http://localhost:8080/swagger-ui.html` (con la app en ejecución)

---

<p align="center"><sub>PasajeYá · Proyecto académico UTP 2026 · Curso Integrador de Sistemas Software</sub></p>
