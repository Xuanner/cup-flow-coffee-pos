package com.cupflow.pos.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.cupflow.pos.TestcontainersConfiguration;
import java.util.List;
import java.util.UUID;
import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseMigrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private DataSource dataSource;

    @Test
    @DisplayName("TC-S1-DATA-001 和 TC-S2-DATA-001 从空 PostgreSQL 初始化到最新结构")
    void initializesIdentityFoundationFromAnEmptyPostgresDatabase() {
        Integer migrationCount = jdbcClient
                .sql("SELECT count(*) FROM flyway_schema_history WHERE success")
                .query(Integer.class)
                .single();
        Integer roleCount = jdbcClient
                .sql("SELECT count(*) FROM roles WHERE code IN ('CASHIER', 'ADMIN')")
                .query(Integer.class)
                .single();
        Integer sessionTableCount = jdbcClient.sql("""
                        SELECT count(*)
                        FROM information_schema.tables
                        WHERE table_schema = 'public' AND table_name = 'auth_sessions'
                        """).query(Integer.class).single();
        List<String> sessionIndexes = jdbcClient.sql("""
                        SELECT indexname
                        FROM pg_indexes
                        WHERE schemaname = 'public' AND tablename = 'auth_sessions'
                        ORDER BY indexname
                        """).query(String.class).list();

        assertThat(migrationCount).isEqualTo(2);
        assertThat(roleCount).isEqualTo(2);
        assertThat(sessionTableCount).isOne();
        assertThat(sessionIndexes)
                .contains(
                        "auth_sessions_pkey",
                        "auth_sessions_token_hash_key",
                        "ix_auth_sessions_account_active",
                        "ix_auth_sessions_idle_expiry_active",
                        "ix_auth_sessions_absolute_expiry_active",
                        "ix_auth_sessions_revoked_cleanup");
    }

    @Test
    @DisplayName("TC-S2-DATA-002 从 V1 升级到最新认证会话结构")
    void upgradesAnExistingV1SchemaWithoutRewritingThePublishedMigration() {
        String schema = uniqueSchema("upgrade");
        try {
            Flyway v1Flyway = flywayFor(schema, MigrationVersion.fromVersion("1"));
            v1Flyway.migrate();

            assertThat(tableExists(schema, "accounts")).isTrue();
            assertThat(tableExists(schema, "auth_sessions")).isFalse();

            Flyway latestFlyway = flywayFor(schema);
            latestFlyway.migrate();

            assertThat(tableExists(schema, "auth_sessions")).isTrue();
            assertThat(successfulMigrationCount(schema)).isEqualTo(2);
        } finally {
            dropSchema(schema);
        }
    }

    @Test
    @DisplayName("TC-S2-DATA-003 重复执行迁移不会重复创建认证对象")
    void repeatedMigrationIsIdempotent() {
        String schema = uniqueSchema("repeat");
        try {
            Flyway flyway = flywayFor(schema);
            flyway.migrate();
            int migrationCountBefore = successfulMigrationCount(schema);

            flyway.migrate();

            assertThat(successfulMigrationCount(schema))
                    .isEqualTo(migrationCountBefore)
                    .isEqualTo(2);
            assertThat(jdbcClient
                            .sql("SELECT count(*) FROM %s.roles WHERE code IN ('CASHIER', 'ADMIN')".formatted(schema))
                            .query(Integer.class)
                            .single())
                    .isEqualTo(2);
        } finally {
            dropSchema(schema);
        }
    }

    @Test
    @DisplayName("TC-S2-DATA-004 会话表只接受摘要并强制时间、撤销和版本约束")
    void sessionStorageRejectsRawCredentialsAndInvalidLifecycleData() {
        String schema = uniqueSchema("constraints");
        try {
            flywayFor(schema).migrate();
            UUID accountId = UUID.randomUUID();
            insertAccount(schema, accountId);

            List<String> columns = jdbcClient
                    .sql("""
                            SELECT column_name
                            FROM information_schema.columns
                            WHERE table_schema = :schema AND table_name = 'auth_sessions'
                            ORDER BY ordinal_position
                            """)
                    .param("schema", schema)
                    .query(String.class)
                    .list();

            assertThat(columns)
                    .contains("token_hash", "last_activity_at", "idle_expires_at", "absolute_expires_at", "version")
                    .doesNotContain("token", "session_token", "raw_token");

            insertValidSession(schema, accountId, "a".repeat(64));

            assertThatThrownBy(() -> insertValidSession(schema, accountId, "raw-browser-credential"))
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcClient
                            .sql("""
                                    INSERT INTO %s.auth_sessions (
                                        id, account_id, token_hash, created_at, last_activity_at,
                                        idle_expires_at, absolute_expires_at, updated_at, version
                                    ) VALUES (
                                        :id, :accountId, :tokenHash,
                                        TIMESTAMPTZ '2026-08-24 08:00:00Z',
                                        TIMESTAMPTZ '2026-08-24 08:01:00Z',
                                        TIMESTAMPTZ '2026-08-24 08:31:00Z',
                                        TIMESTAMPTZ '2026-08-24 08:15:00Z',
                                        TIMESTAMPTZ '2026-08-24 08:01:00Z', 0
                                    )
                                    """.formatted(schema))
                            .param("id", UUID.randomUUID())
                            .param("accountId", accountId)
                            .param("tokenHash", "b".repeat(64))
                            .update())
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcClient
                            .sql("""
                                    UPDATE %s.auth_sessions
                                    SET revocation_reason = 'LOGOUT'
                                    WHERE token_hash = :tokenHash
                                    """.formatted(schema))
                            .param("tokenHash", "a".repeat(64))
                            .update())
                    .isInstanceOf(DataIntegrityViolationException.class);
            assertThatThrownBy(() -> jdbcClient
                            .sql("""
                                    UPDATE %s.auth_sessions
                                    SET version = -1
                                    WHERE token_hash = :tokenHash
                                    """.formatted(schema))
                            .param("tokenHash", "a".repeat(64))
                            .update())
                    .isInstanceOf(DataIntegrityViolationException.class);
        } finally {
            dropSchema(schema);
        }
    }

    private Flyway flywayFor(String schema) {
        return Flyway.configure()
                .dataSource(dataSource)
                .defaultSchema(schema)
                .schemas(schema)
                .createSchemas(true)
                .locations("classpath:db/migration")
                .load();
    }

    private Flyway flywayFor(String schema, MigrationVersion target) {
        return Flyway.configure()
                .dataSource(dataSource)
                .defaultSchema(schema)
                .schemas(schema)
                .createSchemas(true)
                .locations("classpath:db/migration")
                .target(target)
                .load();
    }

    private boolean tableExists(String schema, String tableName) {
        return Boolean.TRUE.equals(jdbcClient
                .sql("""
                        SELECT EXISTS (
                            SELECT 1
                            FROM information_schema.tables
                            WHERE table_schema = :schema AND table_name = :tableName
                        )
                        """)
                .param("schema", schema)
                .param("tableName", tableName)
                .query(Boolean.class)
                .single());
    }

    private int successfulMigrationCount(String schema) {
        return jdbcClient
                .sql("SELECT count(*) FROM %s.flyway_schema_history WHERE success AND version IS NOT NULL"
                        .formatted(schema))
                .query(Integer.class)
                .single();
    }

    private void insertAccount(String schema, UUID accountId) {
        jdbcClient
                .sql("""
                        INSERT INTO %s.accounts (
                            id, username, password_hash, display_name, status
                        ) VALUES (
                            :id, :username, :passwordHash, :displayName, 'ACTIVE'
                        )
                        """.formatted(schema))
                .param("id", accountId)
                .param("username", "migration-test-" + accountId)
                .param("passwordHash", "test-only-password-hash")
                .param("displayName", "Migration Test Account")
                .update();
    }

    private void insertValidSession(String schema, UUID accountId, String tokenHash) {
        jdbcClient
                .sql("""
                        INSERT INTO %s.auth_sessions (
                            id, account_id, token_hash, created_at, last_activity_at,
                            idle_expires_at, absolute_expires_at, updated_at, version
                        ) VALUES (
                            :id, :accountId, :tokenHash,
                            TIMESTAMPTZ '2026-08-24 08:00:00Z',
                            TIMESTAMPTZ '2026-08-24 08:01:00Z',
                            TIMESTAMPTZ '2026-08-24 08:31:00Z',
                            TIMESTAMPTZ '2026-08-24 16:00:00Z',
                            TIMESTAMPTZ '2026-08-24 08:01:00Z', 0
                        )
                        """.formatted(schema))
                .param("id", UUID.randomUUID())
                .param("accountId", accountId)
                .param("tokenHash", tokenHash)
                .update();
    }

    private String uniqueSchema(String purpose) {
        return "s2_%s_%s".formatted(purpose, UUID.randomUUID().toString().replace("-", ""));
    }

    private void dropSchema(String schema) {
        jdbcClient.sql("DROP SCHEMA IF EXISTS %s CASCADE".formatted(schema)).update();
    }
}
