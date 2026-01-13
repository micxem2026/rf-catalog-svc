package me.rightsflow.catalog

import me.rightsflow.intersync.service.BindingControlService
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.CommandLineRunner
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.domain.EntityScan
import org.springframework.boot.runApplication
import org.springframework.cloud.client.discovery.EnableDiscoveryClient
import org.springframework.cloud.openfeign.EnableFeignClients
import org.springframework.data.jpa.repository.config.EnableJpaRepositories
import org.springframework.data.web.config.EnableSpringDataWebSupport


@SpringBootApplication(
    scanBasePackages = [
        "me.rightsflow.catalog",
        "me.rightsflow.features",
        "me.rightsflow.intersync",
        "me.rightsflow.common",
        "me.rightsflow.righttypes",
        "me.rightsflow.oips",
        "me.rightsflow.pge",
        "me.rightsflow.parties",
        "me.rightsflow.clients"
    ]
)
@EnableJpaRepositories(
    basePackages = [
        "me.rightsflow.features.repository",
        "me.rightsflow.intersync.repository",
        "me.rightsflow.righttypes.repository",
        "me.rightsflow.oips.repository",
        "me.rightsflow.parties.repository"
    ]
)
@EntityScan(
    basePackages = [
        "me.rightsflow.features.entity",
        "me.rightsflow.intersync.entity",
        "me.rightsflow.righttypes.entity",
        "me.rightsflow.oips.entity",
        "me.rightsflow.parties.entity"
    ]
)
@EnableDiscoveryClient
@EnableFeignClients(basePackages = ["me.rightsflow.clients.feign"])
@EnableSpringDataWebSupport(pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO)
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
