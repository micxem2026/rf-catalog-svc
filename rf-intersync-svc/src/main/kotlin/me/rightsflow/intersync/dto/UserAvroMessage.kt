package me.rightsflow.intersync.dto

import java.time.Instant

data class UserAvroMessage(
    val id: Int?,
    val username: String,
    val display_name: String,
    val email: String,
    val password_hash: String,
    val enabled: Boolean?,
    val account_non_expired: Boolean?,
    val account_non_locked: Boolean?,
    val expiration_date: Long?,
    val last_logon: Long?,
    val created_at: Long?,
    val updated_at: Long?,
    val user_type: String
)

data class LovSoftwareSystemAvroMessage(
    val id: Int?,
    val name: String
)

data class LovSoftwareObjectAvroMessage(
    val id: Int?,
    val name: String
)

data class KeyMappingAvroMessage(
    val id: Long?,
    val id_sw_sys: Int,
    val id_sw_obj: Int,
    val id_rf: Long,
    val id_ext: Long,
    val created_by: String,
    val created_at: Instant?,
    val updated_by: String?,
    val updated_at: Instant?,
)