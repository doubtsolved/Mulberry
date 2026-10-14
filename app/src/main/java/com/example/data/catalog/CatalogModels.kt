package com.example.data.catalog

import com.google.gson.annotations.SerializedName

data class CatalogResponse(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("lastUpdated") val lastUpdated: Long = 0L,
    @SerializedName("filters") val filters: CatalogFilters = CatalogFilters(),
    @SerializedName("books") val books: List<CatalogBook> = emptyList()
)

data class CatalogFilters(
    @SerializedName("profs") val profs: List<String> = emptyList(),
    @SerializedName("types") val types: List<String> = emptyList(),
    @SerializedName("subjects") val subjects: List<String> = emptyList()
)

data class CatalogBook(
    @SerializedName("id") val id: String = "",
    @SerializedName("title") val title: String = "",
    @SerializedName("author") val author: String = "",
    @SerializedName("edition") val edition: String = "",
    @SerializedName("prof") val prof: String = "",
    @SerializedName("year") val year: String = "",
    @SerializedName("subject") val subject: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("totalPages") val totalPages: Int = 1,
    @SerializedName("sizeMb") val sizeMb: Double = 0.0,
    @SerializedName("coverUrl") val coverUrl: String = "",
    @SerializedName("download") val download: CatalogDownload = CatalogDownload(),
    @SerializedName("uploader") val uploader: String = ""
)

data class CatalogDownload(
    @SerializedName("mirrors") val mirrors: List<CatalogMirror> = emptyList(),
    @SerializedName("telegram") val telegram: CatalogTelegram? = null
)

data class CatalogMirror(
    @SerializedName("provider") val provider: String = "",
    @SerializedName("repo") val repo: String? = null,
    @SerializedName("url") val url: String = "",
    @SerializedName("isEncrypted") val isEncrypted: Boolean = false,
    @SerializedName("format") val format: String? = null
)

data class CatalogTelegram(
    @SerializedName("deepLink") val deepLink: String? = null
)
