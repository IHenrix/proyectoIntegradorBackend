package com.pasajeya.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DatabaseConnection {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConnection.class);

    private static DatabaseConnection instance;

    private final String url      = "jdbc:mysql://localhost:3306/pasajeya_db";
    private final String nombreBD = "pasajeya_db";
    private final int    puerto   = 3306;

    private DatabaseConnection() {
        log.info("[SINGLETON] Instancia creada - url: {}", url);
    }

    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        } else {
            log.info("[SINGLETON] Reutilizando instancia existente #{}", System.identityHashCode(instance));
        }
        return instance;
    }

    public String getUrl()      { return url; }
    public String getNombreBD() { return nombreBD; }
    public int    getPuerto()   { return puerto; }
}
