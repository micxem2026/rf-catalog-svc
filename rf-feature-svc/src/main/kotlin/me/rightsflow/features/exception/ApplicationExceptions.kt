package me.rightsflow.features.exception

class EntityNotFoundException(val entityId: Any) : RuntimeException("Entity not found with id: $entityId")

class CyclicReferenceException(message: String) : RuntimeException(message)