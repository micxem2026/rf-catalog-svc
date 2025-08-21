package me.rightsflow.common.exception

class EntityNotFoundException(val entityId: Any) : RuntimeException("Entity not found with id: $entityId")

class CyclicReferenceException(message: String) : RuntimeException(message)