package com.nwc;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.jboss.logging.Logger;
import org.keycloak.component.ComponentModel;
import org.keycloak.credential.CredentialInput;
import org.keycloak.credential.CredentialInputValidator;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.RealmModel;
import org.keycloak.models.UserCredentialModel;
import org.keycloak.models.UserModel;
import org.keycloak.storage.UserStorageProvider;
import org.keycloak.storage.adapter.AbstractUserAdapterFederatedStorage;
import org.keycloak.storage.user.UserLookupProvider;

public class MariaDBUserFederationProvider implements UserStorageProvider, UserLookupProvider, CredentialInputValidator {
    private static final Logger logger = Logger.getLogger(MariaDBUserFederationProvider.class);

    private final KeycloakSession session;
    private final ComponentModel model;
    private final Connection connection;

    public MariaDBUserFederationProvider(KeycloakSession session, ComponentModel model) {
        this.session = session;
        this.model = model;

        String jdbcUrl = model.getConfig().getFirst("jdbcUrl");
        String dbUser = model.getConfig().getFirst("dbUser");
        String dbPassword = model.getConfig().getFirst("dbPassword");

        logger.debug("MariaDBUserFederationProvider: jdbcUrl=" + jdbcUrl);
        logger.debug("MariaDBUserFederationProvider: dbUser==" + dbUser);

        try {
            Class.forName("org.mariadb.jdbc.Driver");
            this.connection = DriverManager.getConnection(jdbcUrl, dbUser, dbPassword);
        } catch (Exception e) {
            throw new RuntimeException("DB 연결 실패", e);
        }
    }

    @Override
    public boolean supportsCredentialType(String credentialType) {
        logger.debug("MariaDBUserFederationProvider: supportsCredentialType()...credentialType=" + credentialType);
        return UserCredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean isConfiguredFor(RealmModel realmModel, UserModel userModel, String credentialType) {
        logger.debug("MariaDBUserFederationProvider: isConfiguredFor()...credentialType=" + credentialType);
        return UserCredentialModel.PASSWORD.equals(credentialType);
    }

    @Override
    public boolean isValid(RealmModel realmModel, UserModel userModel, CredentialInput credentialInput) {
        logger.debug("MariaDBUserFederationProvider: isValid()...username=" + userModel.getUsername());

        if (!(credentialInput instanceof UserCredentialModel)) return false;

        String inputPassword = ((UserCredentialModel) credentialInput).getChallengeResponse();
        String username = userModel.getUsername();

        String sql = "SELECT password, salt FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String dbPassword = rs.getString("password");
                    String salt = rs.getString("salt");
//                    return dbPassword.equals(inputPassword); // TODO: bcrypt/hash 검증으로 교체

                    boolean result = false;
                    try {
                        result = PBKDF2Util.verifyPassword(inputPassword, salt, dbPassword);
                    } catch (Exception e) {
                        result = false;
                    }
                    return result;
                } else {
                    return false;
                }
            }
        } catch (SQLException e) {
            logger.error("DB 쿼리 실패", e);
            return false;
        }
    }

    @Override
    public UserModel getUserByUsername(RealmModel realmModel, String username) {
        logger.debug("MariaDBUserFederationProvider: getUserByUsername(1)...username=" + username);

        String sql = "SELECT username, email, first_name, last_name FROM users WHERE username = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, username);

            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    logger.debug("MariaDBUserFederationProvider: getUserByUsername(2)...username=" + username);
                    // UserModel을 리턴하면 외부DB 사용자 정보가 -> Keycloak USER_ENTITY에 등록 됨.
//                    UserModel user = session.users().addUser(realmModel, username);
//                    user.setEnabled(true);
//                    user.setEmailVerified(true);
//                    user.setEmail(rs.getString("email"));
//                    user.setFirstName(rs.getString("first_name"));
//                    user.setLastName(rs.getString("last_name"));
//                    return user;
                    // 아래와 같이 리턴하면 Keycloak USER_ENTITY에 등록이 안되고, 외부DB 인증만 수행함.
                    // 그러나 KEYCLOAK 의 기능을 100% 사용할 수 없게 됨.
                    return new AbstractUserAdapterFederatedStorage(session, realmModel, model) {
                        @Override
                        public String getUsername() {
                            logger.debug("MariaDBUserFederationProvider: getUserByUsername(3)...username=" + username);
                            return username;
                        }

                        @Override
                        public void setUsername(String s) {
                        }
                    };
                } else {
                    return null;
                }
            }
        } catch (SQLException e) {
            logger.error("DB 쿼리 실패", e);
            return null;
        }
    }

    @Override
    public UserModel getUserById(RealmModel realmModel, String id) {
        logger.debug("MariaDBUserFederationProvider: getUserById()...id=" + id);

        //id = f:2cb33684-2f5a-4e54-920c-0acaf48053cd:inmokang11
        String[] parts = id.split(":");
        if (parts.length != 3) {
            return null; // 형식이 예상과 다르면 null 반환
        }

        String username = parts[2]; // 실제 사용자명
        return getUserByUsername(realmModel, username); // 재사용
    }

    @Override
    public UserModel getUserByEmail(RealmModel realmModel, String email) {
        logger.debug("MariaDBUserFederationProvider: getUserByEmail()...email=" + email);

        String sql = "SELECT username FROM users WHERE email = ?";

        try (PreparedStatement stmt = connection.prepareStatement(sql)) {
            stmt.setString(1, email);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String username = rs.getString("username");
                    return getUserByUsername(realmModel, username);
                }
            }
        } catch (SQLException e) {
            logger.error("DB 쿼리 실패", e);
        }

        return null;
    }

    @Override
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                logger.debug("DB 연결 종료됨");
            }
        } catch (SQLException e) {
            logger.warn("DB 연결 종료 중 오류 발생", e);
        }
    }
}

