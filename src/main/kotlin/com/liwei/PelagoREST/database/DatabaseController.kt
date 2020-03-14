package com.liwei.PelagoREST.database

import java.sql.Connection
import java.sql.ResultSet

const val APOSTROPHE = "'"
const val DOUBLE_APOSTROPHE = "''"

fun createTable(connection: Connection, schema : String, table : String) {
    val sql = """
        CREATE TABLE $schema.$table (
            ID varchar(40) primary key,
            NAME varchar(30),
            VERSION varchar(10),
            DATE varchar(30),
            TITLE varchar(255),
            DESCRIPTION varchar(max),
            AUTHOR varchar(300),
            MAINTAINER_NAME varchar(300),
            MAINTAINER_EMAIL varchar(300))
        """.trimMargin()

    with(connection) {
        createStatement().execute(sql)
        commit()
    }
}

fun insertRows(connection: Connection, query: String) {
    with(connection) {
        createStatement().execute(query)
        commit()
    }
}

fun insertRow(connection: Connection, vararg inputs: String) {
    with(connection) {
        createStatement().execute(getRowQuery(*inputs))
        commit()
    }
}

fun getRowQuery(vararg inputs: String): String {
    return """
        IF NOT EXISTS (SELECT * FROM ${inputs[0]}.${inputs[1]} WHERE ID = '${inputs[2]}_${inputs[3]}')
        BEGIN
            INSERT INTO ${inputs[0]}.${inputs[1]} (
                ID,
                NAME,
                VERSION,
                DATE,
                TITLE,
                DESCRIPTION,
                AUTHOR,
                MAINTAINER_NAME,
                MAINTAINER_EMAIL
            )
            VALUES(
                '${inputs[2]}_${inputs[3]}',
                '${inputs[2]}',
                '${inputs[3]}',
                '${inputs[4]}',
                '${inputs[5].replace(APOSTROPHE, DOUBLE_APOSTROPHE)}', 
                '${inputs[6].replace(APOSTROPHE, DOUBLE_APOSTROPHE)}', 
                '${inputs[7].replace(APOSTROPHE, DOUBLE_APOSTROPHE)}', 
                '${inputs[8].replace(APOSTROPHE, DOUBLE_APOSTROPHE)}', 
                '${inputs[9]}')
        END;
        """.trimMargin()
}

fun queryRows(connection: Connection, schema : String, table : String, name: String): ResultSet {
    val sql = "SELECT * FROM $schema.$table WHERE NAME='$name';"
    return connection.createStatement().executeQuery(sql)
}
