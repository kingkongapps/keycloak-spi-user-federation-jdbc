package com.nwc;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.models.KeycloakSession;
import org.keycloak.provider.ProviderConfigProperty;
import org.keycloak.storage.UserStorageProviderFactory;

import java.util.ArrayList;
import java.util.List;

public class MariaDBUserFederationProviderFactory implements UserStorageProviderFactory<MariaDBUserFederationProvider> {
    private static final Logger logger = Logger.getLogger(MariaDBUserFederationProviderFactory.class);

    @Override
    public MariaDBUserFederationProvider create(KeycloakSession keycloakSession, ComponentModel componentModel) {
        logger.debug("MariaDBUserFederationProviderFactory: create()...");
        return new MariaDBUserFederationProvider(keycloakSession, componentModel);
    }

    @Override
    public String getId() {
        logger.debug("MariaDBUserFederationProviderFactory: getId()...");
        return "mariadb-user-federation-spi";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        logger.debug("MariaDBUserFederationProviderFactory: getConfigProperties()...");

        List<ProviderConfigProperty> config = new ArrayList<>();

        config.add(new ProviderConfigProperty(
                "jdbcUrl",
                "JDBC URL",
                "JDBC 연결 URL (예: jdbc:mariadb://host:3306/dbname)",
                ProviderConfigProperty.STRING_TYPE,
                "jdbc:mariadb://localhost:3306/keycloak_db"
        ));

        config.add(new ProviderConfigProperty(
                "dbUser",
                "DB 사용자명",
                "데이터베이스 접속 아이디",
                ProviderConfigProperty.STRING_TYPE,
                "keycloak"
        ));

        config.add(new ProviderConfigProperty(
                "dbPassword",
                "DB 비밀번호",
                "데이터베이스 접속 비밀번호",
                ProviderConfigProperty.PASSWORD,
                ""
        ));

        return config;
    }

}
