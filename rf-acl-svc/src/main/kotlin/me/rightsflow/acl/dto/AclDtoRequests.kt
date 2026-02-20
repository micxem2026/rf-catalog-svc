package me.rightsflow.acl.dto

import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.NotNull
import java.time.LocalDate

data class KlfRightTypeRequest(

    @field:NotNull
    var id: Int,

    var idParent: Int?,

    @field:NotEmpty
    val name: String,

    val description: String?,

    @field:NotNull
    var idRightGroup: Int,

    @field:NotNull
    var dropFlag: Boolean
)

data class KlfFeaturePlainRequest(

    @field:NotNull
    var id: Int,

    @field:NotNull
    var idFeatureCategory: Int,

    @field:NotEmpty
    val name: String,

    @field:NotNull
    var dropFlag: Boolean
)

data class KlfFeatureTreeRequest(

    @field:NotNull
    var id: Int,

    var idParent: Int?,

    @field:NotNull
    var idFeatureCategory: Int,

    @field:NotNull
    var idFeaturePlain: Int,

    val begDate: LocalDate?,

    val endDate: LocalDate?,

    @field:NotNull
    var dropFlag: Boolean
)

data class LovOipTypeRequest(

    @field:NotNull
    var id: Int,

    @field:NotNull
    var idOipSuperType: Int,

    @field:NotEmpty
    val name: String,

    @field:NotNull
    var dropFlag: Boolean
)

data class KlfOipRequest(

    @field:NotNull
    var id: Int,

    val guid: String?,

    @field:NotNull
    var idOipSuperType: Int,

    @field:NotNull
    var idOipType: Int,

    @field:NotEmpty
    val name: String,

    val nativeName: String?,

    val fullName: String?,

    val releaseYear: String?,

    @field:NotNull
    var partNum: Int,

    @field:NotNull
    var partCount: Int,

    val duration: String?,

    val description: String?,

    @field:NotNull
    var hasChildren: Boolean,

    @field:NotNull
    var hasParent: Boolean,

    @field:NotNull
    var childrenCount: Int,

    @field:NotNull
    var rootId: Int,

    @field:NotNull
    var dropFlag: Boolean
)

data class KlfOipHierarchyRequest(

    @field:NotNull
    var id: Int,

    @field:NotNull
    var idParent: Int,

    @field:NotNull
    var idOip: Int,

    @field:NotNull
    var dropFlag: Boolean
)

data class KlfOrganizationRequest(

    @field:NotNull
    var id: Int,

    @field:NotEmpty
    var name: String,

    val code1c: String?,
    val country: String?,
    val address: String?,
    val tin: String?,

    @field:NotNull
    var dropFlag: Boolean
)

data class KlfCounterpartyRequest(

    @field:NotNull
    var id: Int,

    @field:NotEmpty
    var name: String,

    val code1c: String?,
    val country: String?,
    val address: String?,
    val tin: String?,
    val idOrgRef: Int?,

    @field:NotNull
    var dropFlag: Boolean
)