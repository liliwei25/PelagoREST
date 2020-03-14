package com.liwei.PelagoREST.controllers

import com.liwei.PelagoREST.database.storePackagesToDatabase
import com.liwei.PelagoREST.models.Contact
import com.liwei.PelagoREST.models.Package
import org.apache.commons.compress.archivers.tar.TarArchiveEntry
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.apache.commons.io.FileUtils
import org.apache.commons.io.IOUtils
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.net.URL
import java.util.Scanner
import java.util.logging.Logger

@RestController
class FetchController {
    val logger: Logger = Logger.getLogger(FetchController::class.toString())

    companion object {
        const val KEY_PATTERN = "[A-Z]\\w+/?\\w+?:"
        const val SEPARATOR_PATTERN = "\\sand\\s|,\\s"
        const val EMAIL_CONTAINER_PATTERN = "[<>]"
        const val TAG_PATTERN = "\\[\\w+,?\\s?\\w+]"
        const val EXTENDED_LINE_PATTERN = "\\n\\s+"
        const val PACKAGES_URL = "http://cran.r-project.org/src/contrib/PACKAGES"
        const val DOWNLOAD_URL = "http://cran.r-project.org/src/contrib/"
        const val DOWNLOAD_FOLDER_PATH = "downloads\\"
        const val EMPTY_STRING = ""
        const val SPACE = " "
        const val PACKAGE_KEY = "Package:"
        const val VERSION_KEY = "Version:"
        const val DATE_KEY = "Date:"
        const val DATE_PUBLICATION_KEY = "Date/Publication:"
        const val TITLE_KEY = "Title:"
        const val DESCRIPTION_KEY = "Description:"
        const val AUTHOR_KEY = "Author:"
        const val MAINTAINER_KEY = "Maintainer:"
        const val DESCRIPTION_FILE_NAME = "DESCRIPTION"
        const val TIMEOUT = 20000
    }

    @GetMapping("/fetch")
    fun fetch(@RequestParam(value = "number", defaultValue = "50") number: Int): List<Package> {
        return try {
            val scanner = Scanner(URL(PACKAGES_URL).openStream())
            val packages = processPackageList(scanner, number)
            storePackagesToDatabase(packages)
            packages
        } catch (ex: IOException) {
            logger.severe(ex.message)
            emptyList()
        }
    }

    fun processPackageList(scanner: Scanner, number: Int): List<Package> {
        val packages: MutableList<Package> = mutableListOf()
        while (scanner.hasNextLine() && packages.size < number) {
            var line: String = scanner.nextLine()
            val lines: MutableMap<String, String> = mutableMapOf()
            while (line.isNotBlank()) {
                val type = Regex(KEY_PATTERN).find(line)
                if (type != null) {
                    val value = line.replace(type.value, EMPTY_STRING).trim()
                    lines += (type.value to value)
                }
                if (!scanner.hasNextLine()) break
                line = scanner.nextLine()
            }
            packages += processPackageData(lines)
        }
        logger.info("Retrieved ${packages.size} packages successfully")
        return packages
    }

    fun processPackageData(data: Map<String, String>): Package {
        val name = data[PACKAGE_KEY]
        val version = data[VERSION_KEY]
        var descriptionData: Map<String, String> = mutableMapOf()
        try {
            val description: String? = fetchPackageDescription(name, version)
            descriptionData = processDescriptionData(description)
            logger.info("Retrieved $name package data successfully")
        } catch (e: IOException) {
            logger.severe(e.message)
        }

        return Package(
            name = name ?: EMPTY_STRING,
            version = version ?: EMPTY_STRING,
            date = descriptionData[DATE_PUBLICATION_KEY] ?: descriptionData[DATE_KEY],
            title = descriptionData[TITLE_KEY],
            description = descriptionData[DESCRIPTION_KEY],
            authors = processAuthorData(descriptionData[AUTHOR_KEY]),
            maintainers = processMaintainerData(descriptionData[MAINTAINER_KEY])
        )
    }

    fun processAuthorData(details: String?): List<Contact> {
        if (details == null) return emptyList()
        val names: List<String> = details.split(Regex(SEPARATOR_PATTERN))
        return names.map { Contact(name = it.trim(), email = null) }
    }

    fun processMaintainerData(details: String?): List<Contact> {
        if (details == null) return emptyList()
        val maintainers: List<String> = details.split(Regex(SEPARATOR_PATTERN))
        return maintainers.map {
            val maintainerDetails = it.split(Regex(EMAIL_CONTAINER_PATTERN))
            Contact(maintainerDetails[0].trim(), maintainerDetails[1].trim())
        }
    }

    fun processDescriptionData(content: String?): Map<String, String> {
        if (content == null) return mapOf()
        val scanner = Scanner(content)
        val values: MutableMap<String, String> = mutableMapOf()
        while (scanner.hasNextLine()) {
            val line = scanner.nextLine().replace(Regex(TAG_PATTERN), EMPTY_STRING)
            val type = Regex(KEY_PATTERN).find(line)
            if (type != null) {
                val value = line.replace(type.value, EMPTY_STRING).trim()
                values += (type.value to value)
            }
        }
        return values
    }

    @Throws(IOException::class)
    fun fetchPackageDescription(name: String?, version: String?): String? {
        val fileName = "${name}_${version}.tar.gz"
        val url = URL(DOWNLOAD_URL + fileName)
        FileUtils.copyURLToFile(url, File(DOWNLOAD_FOLDER_PATH + fileName), TIMEOUT, TIMEOUT)
        return decompress(DOWNLOAD_FOLDER_PATH + fileName)
    }

    @Throws(IOException::class)
    fun decompress(archive: String): String? {
        TarArchiveInputStream(GzipCompressorInputStream(FileInputStream(archive))).use { fin ->
            var entry: TarArchiveEntry
            while (fin.nextTarEntry.also { entry = it } != null) {
                if (entry.name.contains(DESCRIPTION_FILE_NAME)) {
                    return IOUtils.toString(fin, "UTF-8").replace(Regex(EXTENDED_LINE_PATTERN), SPACE)
                }
            }
        }
        return null
    }
}