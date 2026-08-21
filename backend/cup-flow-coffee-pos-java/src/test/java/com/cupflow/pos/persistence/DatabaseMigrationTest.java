package com.cupflow.pos.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.cupflow.pos.TestcontainersConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.simple.JdbcClient;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DatabaseMigrationTest {

    @Autowired
    private JdbcClient jdbcClient;

    @Test
    @DisplayName("TC-S1-DATA-001 从空 PostgreSQL 初始化身份基础")
    void initializesIdentityFoundationFromAnEmptyPostgresDatabase() {
        Integer migrationCount = jdbcClient
                .sql("SELECT count(*) FROM flyway_schema_history WHERE success")
                .query(Integer.class)
                .single();
        Integer roleCount = jdbcClient
                .sql("SELECT count(*) FROM roles WHERE code IN ('CASHIER', 'ADMIN')")
                .query(Integer.class)
                .single();

        assertThat(migrationCount).isPositive();
        assertThat(roleCount).isEqualTo(2);
    }
}
