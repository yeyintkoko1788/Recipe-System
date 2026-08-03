package com.yeyint.recipeapp.backend.config

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.Dispatchers
import org.flywaydb.core.Flyway
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.transactions.experimental.newSuspendedTransaction
import org.slf4j.LoggerFactory
import javax.sql.DataSource

/**
 * Owns the connection pool, schema migrations and the Exposed [Database]
 * handle. Migrations run with Flyway before Exposed touches the schema, so the
 * running code and the database structure can never drift apart silently.
 */
object DatabaseFactory {

    private val log = LoggerFactory.getLogger(javaClass)

    fun init(config: DatabaseConfig): Database {
        val dataSource = hikari(config)
        migrate(dataSource)
        return Database.connect(dataSource)
    }

    private fun hikari(config: DatabaseConfig): DataSource = HikariDataSource(
        HikariConfig().apply {
            jdbcUrl = config.url
            username = config.user
            password = config.password
            maximumPoolSize = config.maxPoolSize
            isAutoCommit = false
            transactionIsolation = "TRANSACTION_REPEATABLE_READ"
            validate()
        },
    )

    private fun migrate(dataSource: DataSource) {
        log.info("Running database migrations…")
        val result = Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load()
            .migrate()
        log.info("Applied {} migration(s)", result.migrationsExecuted)
    }
}

/**
 * Runs [block] inside a suspended database transaction on the IO dispatcher.
 * All repository implementations go through this helper so transaction
 * handling stays in one place.
 */
suspend fun <T> dbQuery(block: suspend () -> T): T =
    newSuspendedTransaction(Dispatchers.IO) { block() }
