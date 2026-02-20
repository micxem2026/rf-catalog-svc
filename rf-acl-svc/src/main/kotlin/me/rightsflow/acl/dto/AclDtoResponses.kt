package me.rightsflow.acl.dto

import me.rightsflow.acl.config.TaskStatus
import java.time.OffsetDateTime

data class InitHierarchyResultDto (
    val updatedCount: Int,
    val executionTimeMs: Long,
    val processedLevels: Int,
    val maxDepth: Int
)

data class BulkLoadOperationResponse(
    val success: Boolean,
    val message: String,
    val timestamp: OffsetDateTime
)

data class TriggersStatusResponse(
    val triggersEnabled: Boolean,
    val triggerStatuses: Map<String, Boolean>,
    val hasRootIdConstraint: Boolean
)

data class BulkLoadTask(
    val id: String,
    val operation: String,
    val status: TaskStatus,
    val startTime: OffsetDateTime,
    val endTime: OffsetDateTime? = null,
    val message: String? = null,
    val error: String? = null
)

data class BulkLoadTaskResponse(
    val taskId: String,
    val status: TaskStatus,
    val message: String? = null,
    val operation: String? = null,
    val startTime: OffsetDateTime? = null,
    val endTime: OffsetDateTime? = null,
    val error: String? = null
)