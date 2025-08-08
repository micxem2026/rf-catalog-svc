package me.rightsflow.catalog

import me.rightsflow.intersync.service.BindingControlService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.data.jpa.repository.config.EnableJpaRepositories


@SpringBootApplication(
    scanBasePackages = [
        "me.rightsflow.catalog",
        "me.rightsflow.features",
        "me.rightsflow.intersync"
    ]
)
@EnableDiscoveryClient
@EnableJpaRepositories(
    basePackages = [
        "me.rightsflow.features.repository",
        "me.rightsflow.intersync.repository"
    ]
)
@EntityScan(
    basePackages = [
        "me.rightsflow.features.entity",
        "me.rightsflow.intersync.entity"
    ]
)
class RfCatalogAppApplication : CommandLineRunner {

    @Autowired
    private lateinit var bindingControlService: BindingControlService

    override fun run(vararg args: String?) {
        bindingControlService.updateBindings()
    }

}

fun main(args: Array<String>) {
    runApplication<RfCatalogAppApplication>(*args)
}
