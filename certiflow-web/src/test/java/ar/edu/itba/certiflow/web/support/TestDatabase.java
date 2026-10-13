package ar.edu.itba.certiflow.web.support;

import org.springframework.jdbc.core.JdbcTemplate;

/** La base H2 de los tests de la API y de los escenarios, y cómo dejarla vacía entre un test y otro. */
public final class TestDatabase {

    public static final String URL =
            "spring.datasource.url=jdbc:h2:mem:certiflow-web;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DEFAULT_NULL_ORDERING=HIGH";
    public static final String USER = "spring.datasource.username=sa";
    public static final String PASSWORD = "spring.datasource.password=";

    private static final String[] TABLES = {"certificate_backing", "audit_entry", "suspension", "certificate", "verification",
            "corrective_action", "finding_evidence", "finding", "revision", "observation", "attached_evidence", "criterion_response",
            "inspection", "schema_version", "inspection_schema", "asset_characteristic", "asset", "revoked_token", "user_account"};

    private TestDatabase() {
    }

    public static void empty(JdbcTemplate jdbcTemplate) {
        for (String table : TABLES) {
            jdbcTemplate.update("delete from " + table);
        }
        jdbcTemplate.execute("alter sequence certificate_number_seq restart with 1");
    }
}
