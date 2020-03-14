package com.liwei.PelagoREST.controllers

import com.liwei.PelagoREST.database.retrievePackageFromDatabase
import com.liwei.PelagoREST.models.Package
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class SearchController {
    @GetMapping("/search")
    fun search(@RequestParam(value = "name", defaultValue = "") name: String): List<Package> {
        return retrievePackageFromDatabase(name)
    }
}