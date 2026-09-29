package com.voikes.technologies.vozo.data

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.voikes.technologies.vozo.db.VozoDatabase
import java.util.Properties

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        JdbcSqliteDriver("jdbc:sqlite:vozo.db", Properties(), VozoDatabase.Schema)
}