package com.liwei.PelagoREST.models

class Package (
    val name: String,
    val version: String,
    val date: String?,
    val title: String?,
    val description: String?,
    val authors: List<Contact>?,
    val maintainers: List<Contact>?
)