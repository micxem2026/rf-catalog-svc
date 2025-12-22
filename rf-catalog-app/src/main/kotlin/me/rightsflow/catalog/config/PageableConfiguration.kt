package me.rightsflow.catalog.config

import org.springframework.context.annotation.Configuration
import org.springframework.data.web.PageableHandlerMethodArgumentResolver
import org.springframework.data.web.config.PageableHandlerMethodArgumentResolverCustomizer


@Configuration
class PageableConfiguration: PageableHandlerMethodArgumentResolverCustomizer {
    override fun customize(pageableResolver: PageableHandlerMethodArgumentResolver) {
        pageableResolver.setOneIndexedParameters(false)
    }

}
