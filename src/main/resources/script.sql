
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
DROP TABLE IF EXISTS aerolinea        CASCADE;
DROP TABLE IF EXISTS suscripcion      CASCADE;
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

CREATE TABLE suscripcion (
  id_suscripcion       INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_persona           INTEGER NOT NULL REFERENCES persona(id_persona),
  id_plan              INTEGER NOT NULL REFERENCES plan(id_plan),
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
  logo_url     VARCHAR(255)
);

CREATE TABLE vuelo (
  id_vuelo     INTEGER GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
  id_aerolinea INTEGER NOT NULL REFERENCES aerolinea(id_aerolinea),
  origen       CHAR(3) NOT NULL,
  destino      CHAR(3) NOT NULL,
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
  precio_objetivo NUMERIC(8,2) NOT NULL CHECK (precio_objetivo > 0),
  activa          BOOLEAN NOT NULL DEFAULT TRUE,
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
CREATE INDEX idx_alerta_usuario   ON alerta(id_usuario, activa);
CREATE INDEX idx_alerta_vuelo     ON alerta(id_vuelo, activa);
CREATE INDEX idx_token_usuario    ON token_verificacion(id_usuario);

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

INSERT INTO aerolinea (nombre, codigo) VALUES
  ('LATAM Airlines Peru', 'LA'),
  ('Sky Airline',         'H2'),
  ('JetSmart',            'JA');

INSERT INTO persona (id_tipo_doc, nro_documento, nombre, apellido_paterno, apellido_materno, genero, telefono) VALUES
  (1, '74405646', 'PEDRO LUIS', 'YARLEQUE', 'LINARES', 'M', '+51999999999');

INSERT INTO usuario (id_persona, id_rol, email, password_hash, proveedor, activo, email_verificado) VALUES
  (1, 3, 'admin@pasajeya.com.pe',
   '$2b$10$T57RmaZUJZkaawlRVyL7puM0x9CfILDd0Iv4cRyisthg/I.Pc4eEW',
   'email', TRUE, TRUE);
   

INSERT INTO vuelo (id_aerolinea, origen, destino, fecha_salida, hora_salida, duracion_min) VALUES
  (1, 'LIM', 'CUZ', '2026-06-20', '06:00', 90),
  (2, 'LIM', 'CUZ', '2026-06-20', '10:15', 90),
  (3, 'LIM', 'CUZ', '2026-06-20', '15:30', 90),
  (1, 'LIM', 'CUZ', '2026-06-20', '18:45', 90),
  (1, 'LIM', 'AQP', '2026-06-20', '07:00', 75),
  (2, 'LIM', 'AQP', '2026-06-20', '11:30', 75),
  (3, 'LIM', 'AQP', '2026-06-20', '16:00', 75),
  (1, 'LIM', 'PIU', '2026-06-20', '08:00', 75),
  (2, 'LIM', 'PIU', '2026-06-20', '13:00', 75),
  (3, 'LIM', 'PIU', '2026-06-20', '17:30', 75);

INSERT INTO tarifa (id_vuelo, tipo, precio, equipaje_bodega_kg, equipaje_mano_kg, costo_cambio_fecha, permite_reembolso, asiento_seleccionable) VALUES
  (1,  'basica',   189.00,  0, 10,  80.00, FALSE, FALSE),
  (1,  'flex',     245.00, 23, 10,   0.00, TRUE,  TRUE),
  (2,  'basica',   145.00,  0, 10,  60.00, FALSE, FALSE),
  (2,  'flex',     195.00, 20, 10,   0.00, TRUE,  TRUE),
  (3,  'basica',   119.00,  0,  8,  70.00, FALSE, FALSE),
  (3,  'flex',     169.00, 20,  8,   0.00, FALSE, TRUE),
  (4,  'basica',   215.00,  0, 10,  80.00, FALSE, FALSE),
  (4,  'flex',     275.00, 23, 10,   0.00, TRUE,  TRUE),
  (4,  'premium',  389.00, 32, 10,   0.00, TRUE,  TRUE),
  (5,  'basica',   175.00,  0, 10,  80.00, FALSE, FALSE),
  (5,  'flex',     229.00, 23, 10,   0.00, TRUE,  TRUE),
  (6,  'basica',   135.00,  0, 10,  60.00, FALSE, FALSE),
  (6,  'flex',     185.00, 20, 10,   0.00, TRUE,  TRUE),
  (7,  'basica',   109.00,  0,  8,  70.00, FALSE, FALSE),
  (7,  'flex',     155.00, 20,  8,   0.00, FALSE, TRUE),
  (8,  'basica',   165.00,  0, 10,  80.00, FALSE, FALSE),
  (8,  'flex',     219.00, 23, 10,   0.00, TRUE,  TRUE),
  (9,  'basica',   128.00,  0, 10,  60.00, FALSE, FALSE),
  (9,  'flex',     178.00, 20, 10,   0.00, TRUE,  TRUE),
  (10, 'basica',    99.00,  0,  8,  70.00, FALSE, FALSE),
  (10, 'flex',     145.00, 20,  8,   0.00, FALSE, TRUE);

INSERT INTO historial_precio (id_vuelo, precio, tipo_tarifa, fecha_captura) VALUES
  (1, 220.00, 'basica', '2026-05-26 06:00:00'),
  (1, 215.00, 'basica', '2026-05-27 06:00:00'),
  (1, 210.00, 'basica', '2026-05-28 06:00:00'),
  (1, 205.00, 'basica', '2026-05-29 06:00:00'),
  (1, 198.00, 'basica', '2026-05-30 06:00:00'),
  (1, 192.00, 'basica', '2026-05-31 06:00:00'),
  (1, 189.00, 'basica', '2026-06-01 06:00:00'),
  (2, 148.00, 'basica', '2026-05-26 10:00:00'),
  (2, 146.00, 'basica', '2026-05-28 10:00:00'),
  (2, 147.00, 'basica', '2026-05-30 10:00:00'),
  (2, 145.00, 'basica', '2026-06-01 10:00:00'),
  (3,  99.00, 'basica', '2026-05-26 15:00:00'),
  (3, 105.00, 'basica', '2026-05-28 15:00:00'),
  (3, 112.00, 'basica', '2026-05-30 15:00:00'),
  (3, 119.00, 'basica', '2026-06-01 15:00:00'),
  (5, 195.00, 'basica', '2026-05-26 07:00:00'),
  (5, 182.00, 'basica', '2026-05-29 07:00:00'),
  (5, 175.00, 'basica', '2026-06-01 07:00:00'),
  (6, 132.00, 'basica', '2026-05-26 11:00:00'),
  (6, 135.00, 'basica', '2026-05-29 11:00:00'),
  (6, 135.00, 'basica', '2026-06-01 11:00:00'),
  (8, 180.00, 'basica', '2026-05-26 08:00:00'),
  (8, 170.00, 'basica', '2026-05-29 08:00:00'),
  (8, 165.00, 'basica', '2026-06-01 08:00:00');
