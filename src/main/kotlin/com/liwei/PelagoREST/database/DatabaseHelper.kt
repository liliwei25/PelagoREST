package com.liwei.PelagoREST.database

import com.liwei.PelagoREST.models.Contact
import com.liwei.PelagoREST.models.Package
import java.sql.Connection
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.SQLException
import java.util.logging.Logger

const val DELIMITER = "|"
const val EMPTY_STRING = ""
const val NAME_KEY = "NAME"
const val VERSION_KEY = "VERSION"
const val DATE_KEY = "DATE"
const val TITLE_KEY = "TITLE"
const val DESCRIPTION_KEY = "DESCRIPTION"
const val AUTHOR_KEY = "AUTHOR"
const val MAINTAINER_NAME_KEY = "MAINTAINER_NAME"
const val MAINTAINER_EMAIL_KEY = "MAINTAINER_EMAIL"
const val SCHEMA = "guest"
const val TABLE = "PACKAGES"
const val SERVER_URL = "jdbc:sqlserver://DESKTOP-B1IUOFU\\MSSQLSERVER:1433"
const val SERVER_USER = "guest"
const val SERVER_PASSWORD = "guest"
val LOGGER: Logger = Logger.getLogger("DatabaseHelper");

fun storePackagesToDatabase(packages: List<Package>) {
    var query = ""
    DriverManager
        .getConnection(SERVER_URL, SERVER_USER, SERVER_PASSWORD)
        .use { connection ->
            checkTable(connection)
            packages.forEach {
                val maintainers: List<String> = serializeMaintainers(it.maintainers)
                query += getRowQuery(SCHEMA, TABLE, it.name, it.version, it.date ?: EMPTY_STRING, it.title ?: EMPTY_STRING,
                    it.description ?: EMPTY_STRING, serializeAuthors(it.authors), maintainers[0], maintainers[1])
            }
            insertRows(connection, query)
        }
}

fun retrievePackageFromDatabase(name: String): List<Package> {
    try {
        DriverManager
            .getConnection(SERVER_URL, SERVER_USER, SERVER_PASSWORD)
            .use { connection ->
                checkTable(connection)
                return parsePackagesFromResult(queryRows(connection, SCHEMA, TABLE, name))
            }
    } catch (e: SQLException) {
        LOGGER.severe(e.message)
        return listOf()
    }
}

fun parsePackagesFromResult(rs: ResultSet): List<Package> {
    val packages: MutableList<Package> = mutableListOf()
    while (rs.next()) {
        packages += Package(
            rs.getString(NAME_KEY),
            rs.getString(VERSION_KEY),
            rs.getString(DATE_KEY),
            rs.getString(TITLE_KEY).replace(DOUBLE_APOSTROPHE, APOSTROPHE),
            rs.getString(DESCRIPTION_KEY).replace(DOUBLE_APOSTROPHE, APOSTROPHE),
            deserializeAuthors(rs.getString(AUTHOR_KEY).replace(DOUBLE_APOSTROPHE, APOSTROPHE)),
            deserializeMaintainers(rs.getString(MAINTAINER_NAME_KEY.replace(DOUBLE_APOSTROPHE, APOSTROPHE)),
                    rs.getString(MAINTAINER_EMAIL_KEY))
        )
    }
    return packages
}

@Throws(SQLException::class)
fun checkTable(connection: Connection) {
    val metaData = connection.metaData
    val rs = metaData.getTables(null, SCHEMA, TABLE, null)

    if (!rs.next()) {
        createTable(connection, SCHEMA, TABLE)
    }
}

fun serializeAuthors(authors: List<Contact>?): String {
    return authors?.joinToString(separator = DELIMITER, transform = {it.name}) ?: EMPTY_STRING
}

fun serializeMaintainers(maintainers: List<Contact>?): List<String> {
    return listOf(maintainers?.joinToString(separator = DELIMITER, transform = {it.name}) ?: EMPTY_STRING,
        maintainers?.joinToString(separator = DELIMITER, transform = {it.email ?: EMPTY_STRING}) ?: EMPTY_STRING)
}

fun deserializeAuthors(data: String): List<Contact> {
    return data.split(DELIMITER).map { Contact(name = it, email = null) }
}

fun deserializeMaintainers(name: String, email: String): List<Contact> {
    return (name.split(DELIMITER) zip email.split(DELIMITER)).map { Contact(name = it.first, email = it.second) }
}