package me.rightsflow.oips.dto.request

import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotNull

@Schema(description = "Создание отношения parent/child для ОИС")
data class OipHierarchyCreateRequest(

    @field:Schema(description = "ID родительского ОИС")
    @field:NotNull
    val idParent: Int,

    @field:Schema(description = "ID ОИС")
    @field:NotNull
    val idOip: Int
)