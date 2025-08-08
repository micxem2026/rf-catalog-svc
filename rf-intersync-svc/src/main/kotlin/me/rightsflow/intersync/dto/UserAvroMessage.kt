package me.rightsflow.intersync.dto

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