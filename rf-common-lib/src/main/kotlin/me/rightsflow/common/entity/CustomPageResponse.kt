package me.rightsflow.common.entity

import org.springframework.data.domain.Page

data class CustomPageResponse<T>(
    val content: List<T>,
    val page: PageMetadata
) {
    data class PageMetadata(
        val size: Int,
        val number: Int,
        val totalElements: Long,
        val totalPages: Int,
        val numberOfElements: Int
    )

    companion object {
        fun <T> of(page: Page<T>): CustomPageResponse<T> {
            return CustomPageResponse(
                content = page.content,
                page = PageMetadata(
                    size = page.size,
                    number = page.number,
                    totalElements = page.totalElements,
                    totalPages = page.totalPages,
                    numberOfElements = page.numberOfElements
                )
            )
        }
    }
}