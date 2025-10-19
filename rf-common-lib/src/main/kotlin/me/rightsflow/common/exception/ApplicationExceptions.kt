package me.rightsflow.common.exception

class EntityNotFoundException(entityId: Any) : RuntimeException("Entity not found [id: $entityId]")
class EntityNotFoundWithClsException(entityId: Any, cls: Class<*>) : RuntimeException("Entity [${cls.simpleName}] for id: $entityId not found")

class CyclicReferenceException(message: String) : RuntimeException(message)
class ConstraintException(entityId: Any, cls: Class<*>) : RuntimeException("Entity [${cls.simpleName}] for id: $entityId has constraint violation")