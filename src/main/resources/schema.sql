-- ============================================================
-- PasajeYa — Esquema PostgreSQL 16
-- Ejecutar una sola vez antes de arrancar el backend
-- ============================================================

-- 1. ROL
CREATE TABLE IF NOT EXISTS rol (
    id_rol   SERIAL PRIMARY KEY,
    nombre   VARCHAR(50) NOT NULL UNIQUE,
    activo   BOOLEAN NOT NULL DEFAULT TRUE
);

-- 2. PERSONA
CREATE TABLE IF NOT EXISTS persona (
    id_persona      SERIAL PRIMARY KEY,
    nombre          VARCHAR(100) NOT NULL,
    apellido        VARCHAR(100) NOT NULL,
    telefono        VARCHAR(20),
    fecha_registro  TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 3. USUARIO
CREATE TABLE IF NOT EXISTS usuario (
    id_usuario       SERIAL PRIMARY KEY,
    id_persona       INTEGER NOT NULL REFERENCES persona(id_persona),
    id_rol           INTEGER NOT NULL REFERENCES rol(id_rol),
    email            VARCHAR(150) NOT NULL UNIQUE,
    password_hash    VARCHAR(255) NOT NULL,
    proveedor        VARCHAR(30) NOT NULL DEFAULT 'email',
    activo           BOOLEAN NOT NULL DEFAULT TRUE,
    email_verificado BOOLEAN NOT NULL DEFAULT FALSE
);

-- 4. TOKEN DE VERIFICACION DE EMAIL
CREATE TABLE IF NOT EXISTS token_verificacion (
    id_token         SERIAL PRIMARY KEY,
    id_usuario       INTEGER NOT NULL REFERENCES usuario(id_usuario),
    token            VARCHAR(255) NOT NULL UNIQUE,
    fecha_creacion   TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_expiracion TIMESTAMP NOT NULL,
    usado            BOOLEAN NOT NULL DEFAULT FALSE
);

-- 5. SESION (para historial de logins — opcional)
CREATE TABLE IF NOT EXISTS sesion (
    id_sesion   SERIAL PRIMARY KEY,
    id_usuario  INTEGER NOT NULL REFERENCES usuario(id_usuario),
    token_jwt   TEXT NOT NULL,
    fecha_inicio TIMESTAMP NOT NULL DEFAULT NOW(),
    fecha_fin    TIMESTAMP,
    activo       BOOLEAN NOT NULL DEFAULT TRUE
);

-- 6. AEROLINEA
CREATE TABLE IF NOT EXISTS aerolinea (
    id_aerolinea  SERIAL PRIMARY KEY,
    nombre        VARCHAR(100) NOT NULL,
    codigo_iata   VARCHAR(5) NOT NULL UNIQUE,
    activo        BOOLEAN NOT NULL DEFAULT TRUE
);

-- 7. VUELO
CREATE TABLE IF NOT EXISTS vuelo (
    id_vuelo      SERIAL PRIMARY KEY,
    id_aerolinea  INTEGER NOT NULL REFERENCES aerolinea(id_aerolinea),
    origen        VARCHAR(5) NOT NULL,
    destino       VARCHAR(5) NOT NULL,
    fecha         DATE NOT NULL,
    hora_salida   TIME NOT NULL,
    hora_llegada  TIME NOT NULL,
    duracion      VARCHAR(20),
    activo        BOOLEAN NOT NULL DEFAULT TRUE
);

-- 8. TARIFA
CREATE TABLE IF NOT EXISTS tarifa (
    id_tarifa        SERIAL PRIMARY KEY,
    id_vuelo         INTEGER NOT NULL REFERENCES vuelo(id_vuelo),
    tipo             VARCHAR(50) NOT NULL,
    precio           NUMERIC(10,2) NOT NULL,
    incluye_equipaje BOOLEAN NOT NULL DEFAULT FALSE,
    semaforo         VARCHAR(10) NOT NULL DEFAULT 'verde'
);

-- 9. PLAN
CREATE TABLE IF NOT EXISTS plan (
    id_plan      SERIAL PRIMARY KEY,
    nombre       VARCHAR(50) NOT NULL,
    precio_mensual NUMERIC(8,2) NOT NULL DEFAULT 0,
    activo       BOOLEAN NOT NULL DEFAULT TRUE
);

-- 10. SUSCRIPCION
CREATE TABLE IF NOT EXISTS suscripcion (
    id_suscripcion  SERIAL PRIMARY KEY,
    id_usuario      INTEGER NOT NULL REFERENCES usuario(id_usuario),
    id_plan         INTEGER NOT NULL REFERENCES plan(id_plan),
    fecha_inicio    DATE NOT NULL DEFAULT CURRENT_DATE,
    fecha_fin       DATE,
    activo          BOOLEAN NOT NULL DEFAULT TRUE
);

-- 11. BUSQUEDA
CREATE TABLE IF NOT EXISTS busqueda (
    id_busqueda  SERIAL PRIMARY KEY,
    id_usuario   INTEGER REFERENCES usuario(id_usuario),
    origen       VARCHAR(5) NOT NULL,
    destino      VARCHAR(5) NOT NULL,
    fecha        DATE NOT NULL,
    pasajeros    INTEGER NOT NULL DEFAULT 1,
    fecha_busqueda TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 12. ALERTA
CREATE TABLE IF NOT EXISTS alerta (
    id_alerta    SERIAL PRIMARY KEY,
    id_usuario   INTEGER NOT NULL REFERENCES usuario(id_usuario),
    origen       VARCHAR(5) NOT NULL,
    destino      VARCHAR(5) NOT NULL,
    precio_maximo NUMERIC(10,2) NOT NULL,
    activo       BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT NOW()
);

-- 13. HISTORIAL DE PRECIO
CREATE TABLE IF NOT EXISTS historial_precio (
    id_historial  SERIAL PRIMARY KEY,
    id_vuelo      INTEGER NOT NULL REFERENCES vuelo(id_vuelo),
    precio        NUMERIC(10,2) NOT NULL,
    fecha_registro TIMESTAMP NOT NULL DEFAULT NOW()
);

-- ============================================================
-- DATOS SEMILLA (necesarios para que el backend arranque)
-- ============================================================

-- Roles
INSERT INTO rol (nombre, activo) VALUES
    ('USER',  TRUE),
    ('ADMIN', TRUE)
ON CONFLICT (nombre) DO NOTHING;

-- Plan gratuito
INSERT INTO plan (nombre, precio_mensual, activo) VALUES
    ('Gratuito', 0.00, TRUE),
    ('Premium',  29.90, TRUE)
ON CONFLICT DO NOTHING;

-- Aerolíneas
INSERT INTO aerolinea (nombre, codigo_iata, activo) VALUES
    ('LATAM Airlines',  'LA', TRUE),
    ('Sky Airline',     'H2', TRUE),
    ('JetSmart',        'FU', TRUE)
ON CONFLICT (codigo_iata) DO NOTHING;

-- ============================================================
-- VERIFICACION FINAL
-- ============================================================
SELECT 'Tablas creadas correctamente' AS resultado;
SELECT 'Roles: ' || COUNT(*) AS info FROM rol;
