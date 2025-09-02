package me.rightsflow.common.exception

class EntityNotFoundException(val entityId: Any) : RuntimeException("Entity not found [id: $entityId]")
class EntityNotFoundWithClsException(val entityId: Any, val cls: Class<*>) : RuntimeException("Entity [${cls.simpleName}] for id: $entityId not found")

class CyclicReferenceException(message: String) : RuntimeException(message)