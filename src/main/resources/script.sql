
-- ============================================================
-- RESET — borra todo en orden inverso
-- ============================================================

DROP TABLE IF EXISTS token_verificacion CASCADE;
DROP TABLE IF EXISTS alerta           CASCADE;
DROP TABLE IF EXISTS consulta_tarifa  CASCADE;
DROP TABLE IF EXISTS busqueda         CASCADE;
DROP TABLE IF EXISTS historial_precio CASCADE;
DROP TABLE IF EXISTS tarifa           CASCADE;
DROP TABLE IF EXISTS vuelo            CASCADE;
DROP TABLE IF EXISTS aeropuerto       CASCADE;
DROP TABLE IF EXISTS aerolinea        CASCADE;
DROP TABLE IF EXISTS suscripcion      CASCADE;
DROP TABLE IF EXISTS pago             CASCADE;
DROP TABLE IF EXISTS sesion           CASCADE;
DROP TABLE IF EXISTS plan             CASCADE;
DROP TABLE IF EXISTS usuario          CASCADE;
DROP TABLE IF EXISTS persona          CASCADE;
DROP TABLE IF EXISTS tipo_documento   CASCADE;
DROP TABLE IF EXISTS rol              CASCADE;

-- PASO 1: Conectate a la BD "postgres" en pgAdmin y ejecuta estas 2 lineas:
-- DROP DATABASE IF EXISTS pasajeya;
-- CREATE DATABASE pasajeya ENCODING = 'UTF8';

-- PASO 2: Conectate a la BD "pasajeya" en pgAdmin y ejecuta el resto del script.

CREATE TABLE rol (
  id_rol      INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre      VARCHAR(50)  NOT NULL UNIQUE
                CHECK (nombre IN ('usuario_free','usuario_premium','admin')),
  descripcion VARCHAR(150)
);

CREATE TABLE tipo_documento (
  id_tipo_doc INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  codigo      VARCHAR(10) NOT NULL UNIQUE,
  nombre      VARCHAR(60) NOT NULL,
  longitud    SMALLINT
);

CREATE TABLE persona (
  id_persona       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_tipo_doc      INTEGER REFERENCES tipo_documento(id_tipo_doc),
  nro_documento    VARCHAR(20) UNIQUE,
  nombre           VARCHAR(100) NOT NULL,
  apellido_paterno VARCHAR(100) NOT NULL,
  apellido_materno VARCHAR(100),
  genero           CHAR(1) CHECK (genero IN ('M','F','O')),
  telefono         VARCHAR(20),
  fecha_nacimiento DATE,
  fecha_registro   TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE usuario (
  id_usuario       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_persona       INTEGER NOT NULL REFERENCES persona(id_persona),
  id_rol           INTEGER NOT NULL DEFAULT 1 REFERENCES rol(id_rol),
  email            VARCHAR(150) NOT NULL UNIQUE,
  password_hash    VARCHAR(255),
  proveedor        VARCHAR(20) NOT NULL DEFAULT 'email'
                     CHECK (proveedor IN ('email','google','apple')),
  activo           BOOLEAN NOT NULL DEFAULT TRUE,
  email_verificado BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE sesion (
  id_sesion        INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_usuario       INTEGER NOT NULL REFERENCES usuario(id_usuario),
  token_hash       VARCHAR(255) NOT NULL,
  fecha_expiracion TIMESTAMP NOT NULL,
  activa           BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE plan (
  id_plan         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre          VARCHAR(50) NOT NULL UNIQUE,
  precio_mensual  NUMERIC(8,2) NOT NULL DEFAULT 0.00,
  duracion_dias   SMALLINT NOT NULL DEFAULT 0,
  max_alertas     SMALLINT NOT NULL DEFAULT 3,
  dias_prediccion SMALLINT NOT NULL DEFAULT 7,
  id_rol          INTEGER NOT NULL REFERENCES rol(id_rol),
  activo          BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE pago (
  id_pago          INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_persona       INTEGER NOT NULL REFERENCES persona(id_persona),
  id_plan          INTEGER NOT NULL REFERENCES plan(id_plan),
  monto            NUMERIC(8,2) NOT NULL,
  moneda           CHAR(3)      NOT NULL DEFAULT 'PEN',
  metodo           VARCHAR(30)  NOT NULL
                     CHECK (metodo IN ('tarjeta_credito','tarjeta_debito','yape','plin','manual')),
  estado           VARCHAR(20)  NOT NULL DEFAULT 'aprobado'
                     CHECK (estado IN ('pendiente','aprobado','rechazado','reembolsado')),
  pasarela         VARCHAR(30)  NOT NULL DEFAULT 'culqi'
                     CHECK (pasarela IN ('culqi','izipay','manual')),
  token_pasarela   VARCHAR(100),           -- charge_id que devuelve Culqi en producción
  ultimos_cuatro   CHAR(4),               -- últimos 4 dígitos de la tarjeta
  marca_tarjeta    VARCHAR(20),           -- visa / mastercard / amex
  titular_tarjeta  VARCHAR(100),
  email_recibo     VARCHAR(150),
  ref_interna      VARCHAR(20) NOT NULL,  -- número estilo #123456 que se muestra al usuario
  fecha_pago       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE suscripcion (
  id_suscripcion       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_persona           INTEGER NOT NULL REFERENCES persona(id_persona),
  id_plan              INTEGER NOT NULL REFERENCES plan(id_plan),
  id_pago_origen       INTEGER REFERENCES pago(id_pago),
  precio_pagado        NUMERIC(8,2) NOT NULL,
  max_alertas_snapshot SMALLINT NOT NULL,
  fecha_inicio         DATE NOT NULL,
  fecha_fin            DATE NOT NULL,
  estado               VARCHAR(20) NOT NULL DEFAULT 'activa'
                         CHECK (estado IN ('activa','vencida','cancelada')),
  metodo_pago          VARCHAR(50)
);

CREATE TABLE aerolinea (
  id_aerolinea INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  nombre       VARCHAR(100) NOT NULL,
  codigo       CHAR(2) NOT NULL UNIQUE,
  logo_url     VARCHAR(255),
  url_web      VARCHAR(255)
);

CREATE TABLE aeropuerto (
  codigo  CHAR(3)      PRIMARY KEY,
  nombre  VARCHAR(100) NOT NULL,
  ciudad  VARCHAR(100) NOT NULL,
  pais    CHAR(2)      NOT NULL DEFAULT 'PE'
);

CREATE TABLE vuelo (
  id_vuelo     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_aerolinea INTEGER NOT NULL REFERENCES aerolinea(id_aerolinea),
  origen       CHAR(3) NOT NULL REFERENCES aeropuerto(codigo),
  destino      CHAR(3) NOT NULL REFERENCES aeropuerto(codigo),
  fecha_salida DATE NOT NULL,
  hora_salida  TIME NOT NULL,
  duracion_min SMALLINT NOT NULL CHECK (duracion_min > 0),
  CONSTRAINT chk_ruta CHECK (origen <> destino)
);

CREATE TABLE tarifa (
  id_tarifa             INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_vuelo              INTEGER NOT NULL REFERENCES vuelo(id_vuelo),
  tipo                  VARCHAR(20) NOT NULL
                          CHECK (tipo IN ('basica','flex','premium')),
  precio                NUMERIC(8,2) NOT NULL CHECK (precio >= 0),
  equipaje_bodega_kg    SMALLINT NOT NULL DEFAULT 0,
  equipaje_mano_kg      SMALLINT NOT NULL DEFAULT 10,
  costo_cambio_fecha    NUMERIC(8,2) NOT NULL DEFAULT 0.00,
  permite_reembolso     BOOLEAN NOT NULL DEFAULT FALSE,
  asiento_seleccionable BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE historial_precio (
  id_historial  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_vuelo      INTEGER NOT NULL REFERENCES vuelo(id_vuelo),
  precio        NUMERIC(8,2) NOT NULL CHECK (precio >= 0),
  tipo_tarifa   VARCHAR(20) NOT NULL
                  CHECK (tipo_tarifa IN ('basica','flex','premium')),
  fecha_captura TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE busqueda (
  id_busqueda    INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_usuario     INTEGER REFERENCES usuario(id_usuario),
  origen         CHAR(3) NOT NULL,
  destino        CHAR(3) NOT NULL,
  fecha_viaje    DATE NOT NULL,
  num_pasajeros  SMALLINT NOT NULL DEFAULT 1
                   CHECK (num_pasajeros BETWEEN 1 AND 9),
  fecha_busqueda TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE consulta_tarifa (
  id_consulta  INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_usuario   INTEGER REFERENCES usuario(id_usuario),
  id_tarifa    INTEGER NOT NULL REFERENCES tarifa(id_tarifa),
  accion       VARCHAR(20) NOT NULL
                 CHECK (accion IN ('vista','redirigido')),
  precio_visto NUMERIC(8,2) NOT NULL,
  url_afiliado VARCHAR(500),
  fecha        TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE alerta (
  id_alerta       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_usuario      INTEGER NOT NULL REFERENCES usuario(id_usuario),
  id_vuelo        INTEGER NOT NULL REFERENCES vuelo(id_vuelo),
  tipo_tarifa     VARCHAR(20) NOT NULL DEFAULT 'basica'
                    CHECK (tipo_tarifa IN ('basica','flex','premium')),
  precio_objetivo NUMERIC(8,2) NOT NULL CHECK (precio_objetivo > 0),
  telefono        VARCHAR(20) NOT NULL,
  activa          BOOLEAN NOT NULL DEFAULT TRUE,
  ultimo_precio_notificado NUMERIC(8,2),
  fecha_ultima_notificacion TIMESTAMP,
  fecha_creacion  TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE token_verificacion (
  id_token         INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_usuario       INTEGER NOT NULL REFERENCES usuario(id_usuario) ON DELETE CASCADE,
  token            VARCHAR(64) NOT NULL UNIQUE,
  fecha_expiracion TIMESTAMP   NOT NULL,
  usado            BOOLEAN     NOT NULL DEFAULT FALSE,
  fecha_creacion   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vuelo_ruta       ON vuelo(origen, destino, fecha_salida);
CREATE INDEX idx_historial_vuelo  ON historial_precio(id_vuelo, fecha_captura);
CREATE INDEX idx_busqueda_usuario ON busqueda(id_usuario, fecha_busqueda);
CREATE INDEX idx_consulta_usuario ON consulta_tarifa(id_usuario, fecha);
CREATE INDEX idx_consulta_accion  ON consulta_tarifa(accion, fecha);
CREATE INDEX idx_alerta_usuario    ON alerta(id_usuario, activa);
CREATE INDEX idx_alerta_vuelo      ON alerta(id_vuelo, activa);
CREATE INDEX idx_token_usuario     ON token_verificacion(id_usuario);
CREATE INDEX idx_suscripcion_persona ON suscripcion(id_persona, estado, fecha_fin DESC);
CREATE INDEX idx_pago_persona      ON pago(id_persona, estado, fecha_pago DESC);

INSERT INTO tipo_documento (codigo, nombre, longitud) VALUES
  ('DNI', 'Documento Nacional de Identidad',  8),
  ('CE',  'Carnet de Extranjeria',            12),
  ('PAS', 'Pasaporte',                      NULL),
  ('RUC', 'Registro Unico de Contribuyentes', 11);

INSERT INTO rol (nombre, descripcion) VALUES
  ('usuario_free',    'Maximo 3 alertas activas, prediccion 7 dias'),
  ('usuario_premium', 'Alertas ilimitadas, prediccion 15 dias'),
  ('admin',           'Acceso completo al sistema');

INSERT INTO plan (nombre, precio_mensual, duracion_dias, max_alertas, dias_prediccion, id_rol) VALUES
  ('Free',            0.00,   0,   3,   7,  1),
  ('Premium Mensual', 15.00,  30,  999, 15, 2),
  ('Premium Anual',   120.00, 365, 999, 15, 2);

INSERT INTO aerolinea (nombre, codigo, url_web) VALUES
  ('LATAM Airlines Peru', 'LA', 'https://www.latam.com/es_pe/'),
  ('Sky Airline',         'H2', 'https://www.skyairline.com/peru'),
  ('JetSmart',            'JA', 'https://jetsmart.com/pe/es/');

INSERT INTO aeropuerto (codigo, nombre, ciudad) VALUES
  ('LIM', 'Jorge Chávez',                    'Lima'),
  ('CUZ', 'Velasco Astete',                  'Cusco'),
  ('AQP', 'Rodríguez Ballón',                'Arequipa'),
  ('PIU', 'Guillermo Concha Ibérico',        'Piura'),
  ('TRU', 'Carlos Martínez de Pinillos',     'Trujillo'),
  ('IQT', 'Francisco Secada Vignetta',       'Iquitos'),
  ('JUL', 'Inca Manco Cápac',               'Juliaca'),
  ('TPP', 'Guillermo del Castillo Paredes',  'Tarapoto'),
  ('TCQ', 'Coronel Carlos Ciriani Santa Rosa','Tacna');

INSERT INTO persona (id_tipo_doc, nro_documento, nombre, apellido_paterno, apellido_materno, genero, telefono, fecha_nacimiento) VALUES
  (1, '74405646', 'PEDRO LUIS',  'YARLEQUE', 'LINARES',  'M', '+51999999999', '1988-03-15'),
  (1, '12345678', 'ENRIQUE',     'PRADA',    'GUERRA',   'M', '+51999888777', '2002-07-22'),
  (1, '47382910', 'JUAN JOSE',   'MORALES',  'VELASQUEZ','M', '+51935430273', '1990-11-08');

-- id_persona=1 admin, id_persona=2 free Enrique, id_persona=3 premium Juan José
-- Todos comparten el mismo password_hash
INSERT INTO usuario (id_persona, id_rol, email, password_hash, proveedor, activo, email_verificado) VALUES
  (1, 3, 'admin@pasajeya.com.pe',
   '$2b$10$T57RmaZUJZkaawlRVyL7puM0x9CfILDd0Iv4cRyisthg/I.Pc4eEW',
   'email', TRUE, TRUE),
  (2, 1, 'enrique.pdg@gmail.com',
   '$2b$10$T57RmaZUJZkaawlRVyL7puM0x9CfILDd0Iv4cRyisthg/I.Pc4eEW',
   'email', TRUE, TRUE),
  (3, 2, 'renrique_prada@hotmail.com',
   '$2b$10$T57RmaZUJZkaawlRVyL7puM0x9CfILDd0Iv4cRyisthg/I.Pc4eEW',
   'email', TRUE, TRUE);
   

-- ============================================================
-- VUELOS + TARIFAS + HISTORIAL — 4 meses (2026-06-08 → 2026-10-08)
-- 8 rutas domésticas Peru basadas en frecuencias reales de Kayak
-- Rutas: CUZ, AQP, PIU, TRU, IQT, JUL, TPP, TCQ (ida+vuelta)
-- Factor dinámico: fin de semana +14%, julio +18%, urgente +28%
-- ~100 vuelos/día → ~12,300 vuelos, ~30,750 tarifas, ~215,250 historial
-- ============================================================
DO $$
DECLARE
  r        RECORD;
  v_id     INTEGER;
  v_factor NUMERIC(6,4);
  v_bas    NUMERIC(8,2);
  v_flex   NUMERIC(8,2);
  v_prem   NUMERIC(8,2);
  i        INTEGER;
BEGIN
  FOR r IN
    SELECT
      h.al_id       AS aerolinea_id,
      h.orig,
      h.dest,
      h.ho::TIME    AS hora,
      h.dur         AS duracion,
      h.pb          AS precio_base,
      f.fecha::DATE AS fecha
    FROM (VALUES
      -- ── LIM → CUZ (LATAM 4x, Sky 2x, JetSMART 2x / día) ────────
      (1,'LIM','CUZ','06:00',90, 205.00),
      (1,'LIM','CUZ','10:15',90, 205.00),
      (1,'LIM','CUZ','15:30',90, 205.00),
      (1,'LIM','CUZ','18:45',90, 205.00),
      (2,'LIM','CUZ','08:30',90, 150.00),
      (2,'LIM','CUZ','14:00',90, 150.00),
      (3,'LIM','CUZ','07:15',90, 122.00),
      (3,'LIM','CUZ','16:30',90, 122.00),
      -- ── CUZ → LIM ───────────────────────────────────────────────
      (1,'CUZ','LIM','07:00',90, 210.00),
      (1,'CUZ','LIM','11:30',90, 210.00),
      (1,'CUZ','LIM','16:45',90, 210.00),
      (1,'CUZ','LIM','19:30',90, 210.00),
      (2,'CUZ','LIM','09:15',90, 155.00),
      (2,'CUZ','LIM','15:00',90, 155.00),
      (3,'CUZ','LIM','08:00',90, 125.00),
      (3,'CUZ','LIM','17:30',90, 125.00),
      -- ── LIM → AQP (LATAM 3x, Sky 2x, JetSMART 2x / día) ────────
      (1,'LIM','AQP','07:00',75, 175.00),
      (1,'LIM','AQP','12:30',75, 175.00),
      (1,'LIM','AQP','17:45',75, 175.00),
      (2,'LIM','AQP','09:00',75, 135.00),
      (2,'LIM','AQP','15:00',75, 135.00),
      (3,'LIM','AQP','11:00',75, 107.00),
      (3,'LIM','AQP','18:30',75, 107.00),
      -- ── AQP → LIM ───────────────────────────────────────────────
      (1,'AQP','LIM','08:15',75, 179.00),
      (1,'AQP','LIM','13:45',75, 179.00),
      (1,'AQP','LIM','19:00',75, 179.00),
      (2,'AQP','LIM','10:00',75, 139.00),
      (2,'AQP','LIM','16:15',75, 139.00),
      (3,'AQP','LIM','12:00',75, 110.00),
      (3,'AQP','LIM','19:30',75, 110.00),
      -- ── LIM → PIU (LATAM 3x, Sky 2x, JetSMART 2x / día) ────────
      (1,'LIM','PIU','08:00',75, 165.00),
      (1,'LIM','PIU','13:00',75, 165.00),
      (1,'LIM','PIU','18:00',75, 165.00),
      (2,'LIM','PIU','10:30',75, 128.00),
      (2,'LIM','PIU','16:30',75, 128.00),
      (3,'LIM','PIU','09:15',75,  97.00),
      (3,'LIM','PIU','17:00',75,  97.00),
      -- ── PIU → LIM ───────────────────────────────────────────────
      (1,'PIU','LIM','09:00',75, 168.00),
      (1,'PIU','LIM','14:30',75, 168.00),
      (1,'PIU','LIM','19:15',75, 168.00),
      (2,'PIU','LIM','11:15',75, 130.00),
      (2,'PIU','LIM','17:30',75, 130.00),
      (3,'PIU','LIM','10:00',75,  99.00),
      (3,'PIU','LIM','18:00',75,  99.00),
      -- ── LIM → TRU Trujillo (LATAM 4x, Sky 2x / día) ─ ruta corta
      (1,'LIM','TRU','06:30',55, 155.00),
      (1,'LIM','TRU','09:45',55, 155.00),
      (1,'LIM','TRU','14:00',55, 155.00),
      (1,'LIM','TRU','18:30',55, 155.00),
      (2,'LIM','TRU','08:00',55, 122.00),
      (2,'LIM','TRU','15:30',55, 122.00),
      -- ── TRU → LIM ───────────────────────────────────────────────
      (1,'TRU','LIM','08:00',55, 158.00),
      (1,'TRU','LIM','11:15',55, 158.00),
      (1,'TRU','LIM','15:30',55, 158.00),
      (1,'TRU','LIM','20:00',55, 158.00),
      (2,'TRU','LIM','09:30',55, 125.00),
      (2,'TRU','LIM','17:00',55, 125.00),
      -- ── LIM → IQT Iquitos (LATAM 3x, Sky 2x / día) ─ ruta amazónica
      (1,'LIM','IQT','07:30',90, 235.00),
      (1,'LIM','IQT','12:00',90, 235.00),
      (1,'LIM','IQT','17:00',90, 235.00),
      (2,'LIM','IQT','09:30',90, 185.00),
      (2,'LIM','IQT','14:30',90, 185.00),
      -- ── IQT → LIM ───────────────────────────────────────────────
      (1,'IQT','LIM','10:00',90, 240.00),
      (1,'IQT','LIM','14:30',90, 240.00),
      (1,'IQT','LIM','19:30',90, 240.00),
      (2,'IQT','LIM','12:00',90, 189.00),
      (2,'IQT','LIM','17:00',90, 189.00),
      -- ── LIM → JUL Juliaca/Puno (LATAM 3x, Sky 2x / día) ────────
      (1,'LIM','JUL','07:00',80, 189.00),
      (1,'LIM','JUL','11:30',80, 189.00),
      (1,'LIM','JUL','17:00',80, 189.00),
      (2,'LIM','JUL','09:00',80, 149.00),
      (2,'LIM','JUL','15:00',80, 149.00),
      -- ── JUL → LIM ───────────────────────────────────────────────
      (1,'JUL','LIM','09:00',80, 192.00),
      (1,'JUL','LIM','13:30',80, 192.00),
      (1,'JUL','LIM','19:00',80, 192.00),
      (2,'JUL','LIM','11:00',80, 152.00),
      (2,'JUL','LIM','17:00',80, 152.00),
      -- ── LIM → TPP Tarapoto (LATAM 3x, Sky 2x, JetSMART 2x / día)
      (1,'LIM','TPP','07:45',70, 169.00),
      (1,'LIM','TPP','12:15',70, 169.00),
      (1,'LIM','TPP','17:30',70, 169.00),
      (2,'LIM','TPP','09:15',70, 132.00),
      (2,'LIM','TPP','15:45',70, 132.00),
      (3,'LIM','TPP','11:00',70, 109.00),
      (3,'LIM','TPP','18:00',70, 109.00),
      -- ── TPP → LIM ───────────────────────────────────────────────
      (1,'TPP','LIM','10:00',70, 172.00),
      (1,'TPP','LIM','14:30',70, 172.00),
      (1,'TPP','LIM','19:45',70, 172.00),
      (2,'TPP','LIM','11:30',70, 135.00),
      (2,'TPP','LIM','18:00',70, 135.00),
      (3,'TPP','LIM','13:15',70, 112.00),
      (3,'TPP','LIM','20:15',70, 112.00),
      -- ── LIM → TCQ Tacna (LATAM 2x, Sky 2x / día) ─ ruta sur ────
      (1,'LIM','TCQ','07:00',120, 219.00),
      (1,'LIM','TCQ','14:30',120, 219.00),
      (2,'LIM','TCQ','09:30',120, 175.00),
      (2,'LIM','TCQ','17:00',120, 175.00),
      -- ── TCQ → LIM ───────────────────────────────────────────────
      (1,'TCQ','LIM','10:00',120, 222.00),
      (1,'TCQ','LIM','17:30',120, 222.00),
      (2,'TCQ','LIM','12:30',120, 178.00),
      (2,'TCQ','LIM','20:00',120, 178.00)
    ) AS h(al_id, orig, dest, ho, dur, pb)
    CROSS JOIN generate_series(
      '2026-06-08'::date,
      '2026-10-08'::date,
      '1 day'::interval
    ) AS f(fecha)
  LOOP
    -- Factor de precio dinámico basado en demanda real
    v_factor := 1.0
      + CASE WHEN EXTRACT(DOW FROM r.fecha) IN (5,6,0) THEN 0.14 ELSE 0 END
      + CASE WHEN EXTRACT(MONTH FROM r.fecha) = 7       THEN 0.18 ELSE 0 END
      + CASE WHEN (r.fecha - CURRENT_DATE) < 7  THEN  0.28
             WHEN (r.fecha - CURRENT_DATE) > 45 THEN -0.10
             WHEN (r.fecha - CURRENT_DATE) > 30 THEN -0.05
             ELSE 0
        END;

    v_bas  := ROUND(r.precio_base * v_factor,        2);
    v_flex := ROUND(r.precio_base * v_factor * 1.35, 2);
    v_prem := ROUND(r.precio_base * v_factor * 1.88, 2);

    INSERT INTO vuelo (id_aerolinea, origen, destino, fecha_salida, hora_salida, duracion_min)
    VALUES (r.aerolinea_id, r.orig, r.dest, r.fecha, r.hora, r.duracion)
    RETURNING id_vuelo INTO v_id;

    -- Tarifa básica — todas las aerolíneas
    INSERT INTO tarifa (id_vuelo, tipo, precio,
                        equipaje_bodega_kg, equipaje_mano_kg,
                        costo_cambio_fecha, permite_reembolso, asiento_seleccionable)
    VALUES (v_id, 'basica', v_bas,
      0,
      CASE r.aerolinea_id WHEN 3 THEN 8 ELSE 10 END,
      CASE r.aerolinea_id WHEN 1 THEN 80.00 WHEN 2 THEN 60.00 ELSE 70.00 END,
      FALSE, FALSE);

    -- Tarifa flex — todas las aerolíneas
    INSERT INTO tarifa (id_vuelo, tipo, precio,
                        equipaje_bodega_kg, equipaje_mano_kg,
                        costo_cambio_fecha, permite_reembolso, asiento_seleccionable)
    VALUES (v_id, 'flex', v_flex,
      CASE r.aerolinea_id WHEN 1 THEN 23 ELSE 20 END,
      CASE r.aerolinea_id WHEN 3 THEN  8 ELSE 10 END,
      0.00, TRUE, TRUE);

    -- Tarifa premium — solo LATAM
    IF r.aerolinea_id = 1 THEN
      INSERT INTO tarifa (id_vuelo, tipo, precio,
                          equipaje_bodega_kg, equipaje_mano_kg,
                          costo_cambio_fecha, permite_reembolso, asiento_seleccionable)
      VALUES (v_id, 'premium', v_prem, 32, 10, 0.00, TRUE, TRUE);
    END IF;

    -- Historial 30 días — 4 perfiles de curva según id_vuelo % 4:
    --   0 = subiendo gradual  (+15% en 30d, demanda creciente)
    --   1 = bajando gradual   (-15% en 30d, asientos disponibles)
    --   2 = volátil           (oscila ±12% cada ~5d, yield management)
    --   3 = estable + spike   (plano, +20% días 15-20, vuelve a base)
    FOR i IN 1..30 LOOP
      DECLARE
        v_factor_hist NUMERIC(8,4);
        v_dia_inv     INTEGER := 31 - i;  -- día 30=más antiguo, día 1=ayer
      BEGIN
        v_factor_hist :=
          CASE v_id % 4
            WHEN 0 THEN
              -- Subiendo: empieza en 0.85 y llega a 1.00 de forma lineal, ruido ±3%
              0.85 + (v_dia_inv::NUMERIC / 30.0) * 0.15
              + (random() * 0.06 - 0.03)
            WHEN 1 THEN
              -- Bajando: empieza en 1.15 y llega a 1.00, ruido ±3%
              1.15 - (v_dia_inv::NUMERIC / 30.0) * 0.15
              + (random() * 0.06 - 0.03)
            WHEN 2 THEN
              -- Volátil: seno con período ~10 días, amplitud 12%, ruido ±2%
              1.0 + 0.12 * SIN(v_dia_inv::NUMERIC * 0.628)
              + (random() * 0.04 - 0.02)
            ELSE
              -- Estable + spike días 10-15 (contando desde hoy hacia atrás)
              CASE WHEN i BETWEEN 10 AND 15
                THEN 1.20 + (random() * 0.06 - 0.03)
                ELSE 1.00 + (random() * 0.06 - 0.03)
              END
          END;

        INSERT INTO historial_precio (id_vuelo, precio, tipo_tarifa, fecha_captura)
        VALUES (
          v_id,
          ROUND((v_bas * GREATEST(v_factor_hist, 0.70))::numeric, 2),
          'basica',
          NOW() - (i * INTERVAL '1 day') - (random() * INTERVAL '6 hours')
        );

        INSERT INTO historial_precio (id_vuelo, precio, tipo_tarifa, fecha_captura)
        VALUES (
          v_id,
          ROUND((v_flex * GREATEST(v_factor_hist, 0.70))::numeric, 2),
          'flex',
          NOW() - (i * INTERVAL '1 day') - (random() * INTERVAL '6 hours')
        );

        IF r.aerolinea_id = 1 THEN
          INSERT INTO historial_precio (id_vuelo, precio, tipo_tarifa, fecha_captura)
          VALUES (
            v_id,
            ROUND((v_prem * GREATEST(v_factor_hist, 0.70))::numeric, 2),
            'premium',
            NOW() - (i * INTERVAL '1 day') - (random() * INTERVAL '6 hours')
          );
        END IF;
      END;
    END LOOP;

  END LOOP;
END $$;

-- ============================================================
-- PAGO + SUSCRIPCIÓN DE JUAN JOSÉ MORALES (id_persona = 3)
-- Plan: Premium Anual (id_plan = 3) · S/ 120.00
-- Pagado: 2025-06-09 con Visa terminada en 4242
-- Vigente: 2025-06-09 → 2026-06-09
-- ============================================================

INSERT INTO pago (
  id_persona, id_plan,
  monto, moneda,
  metodo, estado, pasarela,
  token_pasarela,
  ultimos_cuatro, marca_tarjeta, titular_tarjeta,
  email_recibo,
  ref_interna,
  fecha_pago
) VALUES (
  3, 3,
  120.00, 'PEN',
  'tarjeta_credito', 'aprobado', 'culqi',
  'chr_test_xK9mP2qR7nL4sW8vA1bC3dE6',
  '4242', 'visa', 'JUAN JOSE MORALES VELASQUEZ',
  'renrique_prada@hotmail.com',
  '748291',
  '2025-06-09 14:23:07'
);

INSERT INTO suscripcion (
  id_persona, id_plan, id_pago_origen,
  precio_pagado, max_alertas_snapshot,
  fecha_inicio, fecha_fin,
  estado, metodo_pago
)
SELECT
  3, 3, id_pago,
  120.00, 999,
  '2025-06-09', '2026-06-09',
  'activa', 'tarjeta_credito'
FROM pago
WHERE ref_interna = '748291';
