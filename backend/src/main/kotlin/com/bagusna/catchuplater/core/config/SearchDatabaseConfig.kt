package com.bagusna.catchuplater.core.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import java.nio.file.Files
import java.nio.file.Path
import javax.sql.DataSource

/**
 * Builds the sidecar SQLite database that holds the FTS5 search index.
 *
 * The index is derived data and lives in its own file so the virtual table
 * never appears in the main schema. This keeps `ddl-auto=validate` working:
 * SQLite reports FTS5 columns with an empty type, which Hibernate's schema
 * extractor cannot parse.
 *
 * The [SQLiteDataSource][DriverManagerDataSource] is intentionally not a
 * Spring bean, so Spring Boot's DataSource/JPA auto-configuration still sees a
 * single primary datasource.
 */
@Configuration
class SearchDatabaseConfig {

    @Bean
    fun searchJdbcTemplate(@Value("\${app.search.database-path}") databasePath: String): JdbcTemplate {
        val resolved = Path.of(databasePath).toAbsolutePath().normalize()
        resolved.parent?.let { Files.createDirectories(it) }

        val dataSource = DriverManagerDataSource().apply {
            setDriverClassName("org.sqlite.JDBC")
            url = "jdbc:sqlite:$resolved"
        }
        val jdbc = JdbcTemplate(dataSource)
        jdbc.execute("PRAGMA journal_mode=WAL")
        jdbc.execute("PRAGMA busy_timeout=5000")
        jdbc.execute(
            """
            CREATE VIRTUAL TABLE IF NOT EXISTS content_search USING fts5(
                owner_id UNINDEXED,
                content_item_id UNINDEXED,
                artifact_id UNINDEXED,
                title,
                description,
                author,
                source_name,
                body_text,
                tags,
                tokenize = 'unicode61'
            )
            """.trimIndent(),
        )
        return jdbc
    }

    /**
     * Explicit `NamedParameterJdbcTemplate` for the main (application)
     * database. Declaring the search [JdbcTemplate] above makes Spring Boot's
     * `JdbcTemplateAutoConfiguration` back off, so this bean is supplied here.
     */
    @Bean
    fun namedParameterJdbcTemplate(dataSource: DataSource): NamedParameterJdbcTemplate =
        NamedParameterJdbcTemplate(dataSource)
}
