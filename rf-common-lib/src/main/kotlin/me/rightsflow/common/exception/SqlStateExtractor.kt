package me.rightsflow.common.exception

import org.postgresql.util.PSQLException

object SqlStateExtractor {
    fun extractSqlState(t: Throwable?): Pair<String, String>? {
        var cur = t
        while (cur != null) {
            if (cur is PSQLException) return Pair(cur.sqlState, cur.message?:"Unknown SQL Error")
            cur = cur.cause
        }
        return null
    }
}