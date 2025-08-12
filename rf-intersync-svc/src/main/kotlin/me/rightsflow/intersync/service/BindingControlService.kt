package me.rightsflow.intersync.service

import me.rightsflow.intersync.repository.KafkaBindingControlRepository
import org.slf4j.LoggerFactory
import org.springframework.cloud.stream.binding.BindingService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BindingControlService(
    private val bindingService: BindingService,
    private val repository: KafkaBindingControlRepository
) {
    private val log = LoggerFactory.getLogger(BindingControlService::class.java)

    @Transactional(readOnly = true)
    @Scheduled(cron = "0/\${rightsflow.app.binding-control-poll-seconds} * * * * *")
    fun updateBindings() {
        repository.findAll().forEach { control ->
            val bindings = bindingService.getConsumerBindings(control.bindingName)
            for (binding in bindings) {
                if (binding == null) {
                    log.warn("Binding ${control.bindingName} not found")
                    continue
                }
                when (control.bindingState.name) {
                    "PAUSE" -> if (!binding.isPaused) {
                        binding.pause()
                        log.debug("Binding ${control.bindingName} state updated to ${control.bindingState}")
                    }

                    "RESUME" -> if (binding.isPaused) {
                        binding.resume()
                        log.debug("Binding ${control.bindingName} state updated to ${control.bindingState}")
                    }
                }
            }
        }
    }
}