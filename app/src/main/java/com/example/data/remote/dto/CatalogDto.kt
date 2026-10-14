package com.example.data.remote.dto

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class CatalogResponseDto(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("lastUpdated") val lastUpdated: Long = 0L,
    @SerializedName("filters") val filters: FiltersDto? = null,
    @SerializedName("books") val books: List<BookDto> = emptyList()
)

@Keep
data class FiltersDto(
    @SerializedName("profs") val profs: List<String> = emptyList(),
    @SerializedName("types") val types: List<String> = emptyList(),
    @SerializedName("subjects") val subjects: List<String> = emptyList()
)

@Keep
data class BookDto(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String = "Medical Scholar",
    @SerializedName("edition") val edition: String = "Latest Edition",
    @SerializedName("prof") val prof: String = "1st Prof",
    @SerializedName("year") val year: String = "",
    @SerializedName("subject") val subject: String = "General",
    @SerializedName("type") val type: String = "Standard Textbook",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("totalPages") val totalPages: Int = 1,
    @SerializedName("sizeMb") val sizeMb: Double = 0.0,
    @SerializedName("coverUrl") val coverUrl: String = "",
    @SerializedName("download") val download: DownloadDto = DownloadDto(),
    @SerializedName("uploader") val uploader: String = "Mulberry"
)

@Keep
data class DownloadDto(
    @SerializedName("mirrors") val mirrors: List<MirrorDto> = emptyList(),
    @SerializedName("telegram") val telegram: TelegramDto? = null
)

@Keep
data class MirrorDto(
    @SerializedName("provider") val provider: String = "github",
    @SerializedName("repo") val repo: String = "",
    @SerializedName("url") val url: String = "",
    @SerializedName("isEncrypted") val isEncrypted: Boolean = true,
    @SerializedName("format") val format: String = "aes-256-gcm"
)

@Keep
data class TelegramDto(
    @SerializedName("deepLink") val deepLink: String = ""
)
