package com.yeyint.recipeapp.backend.util

import io.ktor.server.application.ApplicationCall

/** Sort direction parsed from `?order=asc|desc`. */
enum class SortDirection { ASC, DESC }

/**
 * Normalized pagination + sorting parameters. [sortBy] is validated against a
 * per-endpoint whitelist so clients can never sort by arbitrary columns.
 */
data class PageRequest(
    val page: Int,
    val size: Int,
    val sortBy: String,
    val direction: SortDirection,
) {
    val offset: Long get() = page.toLong() * size
}

const val MAX_PAGE_SIZE = 100

/**
 * Parses `page`, `size`, `sort` and `order` query parameters.
 *
 * @param allowedSorts whitelist of sortable field names for this endpoint.
 * @param defaultSort must be a member of [allowedSorts].
 */
fun ApplicationCall.pageRequest(
    allowedSorts: Set<String>,
    defaultSort: String,
): PageRequest {
    val page = (parameters["page"]?.toIntOrNull() ?: 0).coerceAtLeast(0)
    val size = (parameters["size"]?.toIntOrNull() ?: 20).coerceIn(1, MAX_PAGE_SIZE)
    val sort = parameters["sort"]?.takeIf { it in allowedSorts }
        ?: defaultSort
    val direction = when (parameters["order"]?.lowercase()) {
        "asc" -> SortDirection.ASC
        "desc" -> SortDirection.DESC
        else -> SortDirection.DESC
    }
    return PageRequest(page, size, sort, direction)
}
