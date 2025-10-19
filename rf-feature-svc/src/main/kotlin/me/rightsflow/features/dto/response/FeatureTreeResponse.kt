package me.rightsflow.features.dto.response

import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime

interface FeatureTreeProjection {
    val id: Int
    val idParent: Int?
    val idFeatureCategory: Int
    val categoryName: String
    val idFeaturePlain: Int
    val plainName: String
    val beginDate: LocalDate?
    val endDate: LocalDate?
    val createdBy: String
    val createdAt: LocalDateTime
    val updatedBy: String?
    val updatedAt: LocalDateTime?
}

@Schema(description = "Ответ с данными характеристики в дереве")
data class FeatureTreeResponse(
    @field:Schema(description = "ID характеристики", example = "1")
    override val id: Int,

    @field:Schema(description = "ID родительского элемента", example = "1")
    override val idParent: Int?,

    @field:Schema(description = "ID категории характеристики", example = "1")
    override val idFeatureCategory: Int,

    @field:Schema(description = "Название категории характеристики", example = "Язык")
    override val categoryName: String,

    @field:Schema(description = "ID простой характеристики", example = "5")
    override val idFeaturePlain: Int,

    @field:Schema(description = "Название простой характеристики", example = "Русский")
    override val plainName: String,

    @field:Schema(description = "Дата начала периода действия элемента. Если указан NULL, то дата начала периода не имеет ограничения снизу", example = "2022-01-01")
    override val beginDate: LocalDate?,

    @field:Schema(description = "Дата окончания периода действия элемента. Если указан NULL, то дата окончания периода не имеет ограничения сверху", example = "2025-01-01")
    override val endDate: LocalDate?,

    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    override val createdBy: String,

    @field:Schema(description = "Дата и время создания записи")
    override val createdAt: LocalDateTime,

    @field:Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    override val updatedBy: String?,

    @field:Schema(description = "Дата и время последнего изменения записи")
    override val updatedAt: LocalDateTime?
): FeatureTreeProjection

interface FeatureTreeRecursiveProjection {
    val id: Int
    val idParent: Int?
    val idFeatureCategory: Int
    val categoryName: String
    val idFeaturePlain: Int
    val plainName: String
    val beginDate: LocalDate?
    val endDate: LocalDate?
    val createdBy: String
    val createdAt: LocalDateTime
    val updatedBy: String?
    val updatedAt: LocalDateTime?
    val children: List<FeatureTreeRecursiveProjection>
}

@Schema(description = "Ответ с данными характеристики в дереве в рекурсивном режиме")
data class FeatureTreeRecursiveResponse(
    @field:Schema(description = "ID характеристики", example = "1")
    override val id: Int,

    @field:Schema(description = "ID родительского элемента", example = "1")
    override val idParent: Int?,

    @field:Schema(description = "ID категории характеристики", example = "1")
    override val idFeatureCategory: Int,

    @field:Schema(description = "Название категории характеристики", example = "Язык")
    override val categoryName: String,

    @field:Schema(description = "ID простой характеристики", example = "5")
    override val idFeaturePlain: Int,

    @field:Schema(description = "Название простой характеристики", example = "Русский")
    override val plainName: String,

    @field:Schema(description = "Дата начала периода действия элемента. Если указан NULL, то дата начала периода не имеет ограничения снизу", example = "2022-01-01")
    override val beginDate: LocalDate?,

    @field:Schema(description = "Дата окончания периода действия элемента. Если указан NULL, то дата окончания периода не имеет ограничения сверху", example = "2025-01-01")
    override val endDate: LocalDate?,

    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    override val createdBy: String,

    @field:Schema(description = "Дата и время создания записи")
    override val createdAt: LocalDateTime,

    @field:Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    override val updatedBy: String?,

    @field:Schema(description = "Дата и время последнего изменения записи")
    override val updatedAt: LocalDateTime?,

    @field:Schema(description = "Список дочерних элементов")
    override val children: List<FeatureTreeRecursiveProjection> = emptyList()
): FeatureTreeRecursiveProjection


interface FeatureTreePlainProjection {
    val id: Int
    val idParent: Int?
    val idFeatureCategory: Int
    val categoryName: String
    val idFeaturePlain: Int
    val plainName: String
    val beginDate: LocalDate?
    val endDate: LocalDate?
    val createdBy: String
    val createdAt: LocalDateTime
    val updatedBy: String?
    val updatedAt: LocalDateTime?
    val level: Int
}

@Schema(description = "Ответ с данными характеристики в дереве в плоском режиме")
data class FeatureTreePlainResponse (
    @field:Schema(description = "ID характеристики", example = "1")
    override val id: Int,

    @field:Schema(description = "ID родительского элемента", example = "1")
    override val idParent: Int?,

    @field:Schema(description = "ID категории характеристики", example = "1")
    override val idFeatureCategory: Int,

    @field:Schema(description = "Название категории характеристики", example = "Язык")
    override val categoryName: String,

    @field:Schema(description = "ID простой характеристики", example = "5")
    override val idFeaturePlain: Int,

    @field:Schema(description = "Название простой характеристики", example = "Русский")
    override val plainName: String,

    @field:Schema(description = "Дата начала периода действия элемента. Если указан NULL, то дата начала периода не имеет ограничения снизу", example = "2022-01-01")
    override val beginDate: LocalDate?,

    @field:Schema(description = "Дата окончания периода действия элемента. Если указан NULL, то дата окончания периода не имеет ограничения сверху", example = "2025-01-01")
    override val endDate: LocalDate?,

    @field:Schema(description = "Пользователь, создавший запись", example = "admin")
    override val createdBy: String,

    @field:Schema(description = "Дата и время создания записи")
    override val createdAt: LocalDateTime,

    @field:Schema(description = "Пользователь, последний изменивший запись", example = "admin")
    override val updatedBy: String?,

    @field:Schema(description = "Дата и время последнего изменения записи")
    override val updatedAt: LocalDateTime?,

    @field:Schema(description = "Уровень вложенности (начинается с 1)", example = "1")
    override val level: Int = 0
): FeatureTreePlainProjection
