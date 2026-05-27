package me.rightsflow.common.util

import java.time.LocalDate
import io.hypersistence.utils.hibernate.type.range.Range
import jakarta.persistence.EntityManager
import jakarta.persistence.Query
import org.hibernate.Session
import org.hibernate.query.NativeQuery

fun Range<LocalDate>.realUpper(): LocalDate? =
    if (upper() == null) null else if (!isUpperBoundClosed) upper().minusDays(1) else upper()

fun Range<LocalDate>.realLower(): LocalDate? =
    if (lower() == null) null else if (!isLowerBoundClosed) lower().plusDays(1) else lower()

inline fun <reified T : Enum<T>> String.toEnumOrNull(): T? =
    enumValues<T>().find { it.name == this }

/**
 * Устанавливает параметр типа TEXT[] в нативный JPA-запрос.
 * Hibernate не поддерживает java.sql.Array напрямую через setParameter,
 * поэтому используем doWork для получения физического Connection.
 */
fun Query.setTextArrayParam(
    em: EntityManager,
    paramName: String,
    values: Array<String>
) {
    val session = em.unwrap(Session::class.java)
    session.doWork { connection ->
        val sqlArray = connection.createArrayOf("text", values)
        this.unwrap(NativeQuery::class.java).setParameter(paramName, sqlArray)
    }
}