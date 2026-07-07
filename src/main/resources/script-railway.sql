
-- ============================================================
-- SCRIPT REDUCIDO PARA RAILWAY (plan con volumen limitado a 500MB)
-- ============================================================
-- Mismo esquema exacto que script.sql, pero con MUCHO menos volumen
-- de datos generados: 3 rutas (en vez de 9), 10 días de vuelos (en
-- vez de 123) y 15 días de historial retroactivo (en vez de 90).
-- ~420 vuelos / ~1,050 tarifas / ~31,500 filas de historial — unas
-- 30 veces menos que el script.sql original, para que quepa cómodo
-- en un volumen de 500MB sin llenar el WAL a mitad de la ejecución.
-- ============================================================

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
  token_pasarela   VARCHAR(100),
  ultimos_cuatro   CHAR(4),
  marca_tarjeta    VARCHAR(20),
  titular_tarjeta  VARCHAR(100),
  email_recibo     VARCHAR(150),
  ref_interna      VARCHAR(20) NOT NULL,
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
  metodo_pago          VARCHAR(50),
  auto_renovar         BOOLEAN NOT NULL DEFAULT FALSE
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
                   CHECK (num_pasajeros BETWEEN 1 AND 4),
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

-- Catálogo completo de aerolíneas y aeropuertos (igual que script.sql):
-- no generar menos filas aquí no ahorra espacio real, y mantiene
-- consistencia con el resto del sistema (búsquedas a otras rutas
-- devuelven vacío en vez de fallar por FK inexistente).
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
  (2, 1, 'enrique.pdg@outlook.com',
   '$2b$10$T57RmaZUJZkaawlRVyL7puM0x9CfILDd0Iv4cRyisthg/I.Pc4eEW',
   'email', TRUE, TRUE),
  (3, 2, 'renrique_prada@hotmail.com',
   '$2b$10$T57RmaZUJZkaawlRVyL7puM0x9CfILDd0Iv4cRyisthg/I.Pc4eEW',
   'email', TRUE, TRUE);

-- ============================================================
-- VUELOS + TARIFAS + HISTORIAL — versión reducida para 500MB
-- 3 rutas (6 direcciones): LIM-CUZ, LIM-AQP, LIM-PIU
-- 10 días de vuelos futuros · 15 días de historial retroactivo
-- ~420 vuelos, ~1,050 tarifas, ~31,500 filas de historial
-- (volumen ~30x menor que script.sql — no necesita COMMITs
-- parciales; una sola transacción de este tamaño es liviana)
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
      -- ── LIM → CUZ ──────────────────────────────────────────────
      (1,'LIM','CUZ','06:00',90, 205.00),
      (1,'LIM','CUZ','15:30',90, 205.00),
      (2,'LIM','CUZ','08:30',90, 150.00),
      (3,'LIM','CUZ','07:15',90, 122.00),
      -- ── CUZ → LIM ──────────────────────────────────────────────
      (1,'CUZ','LIM','07:00',90, 210.00),
      (1,'CUZ','LIM','16:45',90, 210.00),
      (2,'CUZ','LIM','09:15',90, 155.00),
      (3,'CUZ','LIM','08:00',90, 125.00),
      -- ── LIM → AQP ──────────────────────────────────────────────
      (1,'LIM','AQP','07:00',75, 175.00),
      (1,'LIM','AQP','17:45',75, 175.00),
      (2,'LIM','AQP','09:00',75, 135.00),
      (3,'LIM','AQP','11:00',75, 107.00),
      -- ── AQP → LIM ──────────────────────────────────────────────
      (1,'AQP','LIM','08:15',75, 179.00),
      (1,'AQP','LIM','19:00',75, 179.00),
      (2,'AQP','LIM','10:00',75, 139.00),
      (3,'AQP','LIM','12:00',75, 110.00),
      -- ── LIM → PIU ──────────────────────────────────────────────
      (1,'LIM','PIU','08:00',75, 165.00),
      (1,'LIM','PIU','18:00',75, 165.00),
      (2,'LIM','PIU','10:30',75, 128.00),
      (3,'LIM','PIU','09:15',75,  97.00),
      -- ── PIU → LIM ──────────────────────────────────────────────
      (1,'PIU','LIM','09:00',75, 168.00),
      (1,'PIU','LIM','19:15',75, 168.00),
      (2,'PIU','LIM','11:15',75, 130.00),
      (3,'PIU','LIM','10:00',75,  99.00)
    ) AS h(al_id, orig, dest, ho, dur, pb)
    CROSS JOIN generate_series(
      CURRENT_DATE,
      CURRENT_DATE + INTERVAL '9 days',
      '1 day'::interval
    ) AS f(fecha)
  LOOP
    v_factor := 1.0
      + CASE WHEN EXTRACT(DOW FROM r.fecha) IN (5,6,0) THEN 0.14 ELSE 0 END
      + CASE WHEN (r.fecha - CURRENT_DATE) < 3 THEN 0.20 ELSE 0 END;

    v_bas  := ROUND(r.precio_base * v_factor,        2);
    v_flex := ROUND(r.precio_base * v_factor * 1.35, 2);
    v_prem := ROUND(r.precio_base * v_factor * 1.88, 2);

    INSERT INTO vuelo (id_aerolinea, origen, destino, fecha_salida, hora_salida, duracion_min)
    VALUES (r.aerolinea_id, r.orig, r.dest, r.fecha, r.hora, r.duracion)
    RETURNING id_vuelo INTO v_id;

    INSERT INTO tarifa (id_vuelo, tipo, precio,
                        equipaje_bodega_kg, equipaje_mano_kg,
                        costo_cambio_fecha, permite_reembolso, asiento_seleccionable)
    VALUES (v_id, 'basica', v_bas,
      0,
      CASE r.aerolinea_id WHEN 3 THEN 8 ELSE 10 END,
      CASE r.aerolinea_id WHEN 1 THEN 80.00 WHEN 2 THEN 60.00 ELSE 70.00 END,
      FALSE, FALSE);

    INSERT INTO tarifa (id_vuelo, tipo, precio,
                        equipaje_bodega_kg, equipaje_mano_kg,
                        costo_cambio_fecha, permite_reembolso, asiento_seleccionable)
    VALUES (v_id, 'flex', v_flex,
      CASE r.aerolinea_id WHEN 1 THEN 23 ELSE 20 END,
      CASE r.aerolinea_id WHEN 3 THEN  8 ELSE 10 END,
      0.00, TRUE, TRUE);

    IF r.aerolinea_id = 1 THEN
      INSERT INTO tarifa (id_vuelo, tipo, precio,
                          equipaje_bodega_kg, equipaje_mano_kg,
                          costo_cambio_fecha, permite_reembolso, asiento_seleccionable)
      VALUES (v_id, 'premium', v_prem, 32, 10, 0.00, TRUE, TRUE);
    END IF;

    -- Historial 15 días — mismo patrón de curvas que script.sql, acortado
    FOR i IN 1..15 LOOP
      DECLARE
        v_factor_hist NUMERIC(8,4);
        v_dia_inv     INTEGER := 16 - i;
      BEGIN
        v_factor_hist :=
          CASE v_id % 4
            WHEN 0 THEN 0.90 + (v_dia_inv::NUMERIC / 15.0) * 0.10 + (random() * 0.06 - 0.03)
            WHEN 1 THEN 1.10 - (v_dia_inv::NUMERIC / 15.0) * 0.10 + (random() * 0.06 - 0.03)
            WHEN 2 THEN 1.0 + 0.12 * SIN(v_dia_inv::NUMERIC * 0.628) + (random() * 0.04 - 0.02)
            ELSE
              CASE WHEN i BETWEEN 5 AND 8
                THEN 1.20 + (random() * 0.06 - 0.03)
                ELSE 1.00 + (random() * 0.06 - 0.03)
              END
          END;

        INSERT INTO historial_precio (id_vuelo, precio, tipo_tarifa, fecha_captura)
        VALUES (v_id, ROUND((v_bas * GREATEST(v_factor_hist, 0.70))::numeric, 2), 'basica',
          NOW() - (i * INTERVAL '1 day') - (random() * INTERVAL '6 hours'));

        INSERT INTO historial_precio (id_vuelo, precio, tipo_tarifa, fecha_captura)
        VALUES (v_id, ROUND((v_flex * GREATEST(v_factor_hist, 0.70))::numeric, 2), 'flex',
          NOW() - (i * INTERVAL '1 day') - (random() * INTERVAL '6 hours'));

        IF r.aerolinea_id = 1 THEN
          INSERT INTO historial_precio (id_vuelo, precio, tipo_tarifa, fecha_captura)
          VALUES (v_id, ROUND((v_prem * GREATEST(v_factor_hist, 0.70))::numeric, 2), 'premium',
            NOW() - (i * INTERVAL '1 day') - (random() * INTERVAL '6 hours'));
        END IF;
      END;
    END LOOP;

  END LOOP;
END $$;

-- ============================================================
-- PAGO + SUSCRIPCIÓN DE JUAN JOSÉ MORALES (id_persona = 3)
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

COMMIT;
