package me.rightsflow.intersync.entity

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.SequenceGenerator
import jakarta.persistence.Table
import java.time.LocalDateTime

@Entity
@Table(name = "kafka_bindings_control")
data class KafkaBindingControl(

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "bindings_control_seq")
    @SequenceGenerator(name = "bindings_control_seq", sequenceName = "kafka_bindings_control_id_seq", allocationSize = 1)
    val id: Int,

    @Column(name = "binding_name", nullable = false, length = 255)
    val bindingName: String,

    @Column(name = "binding_state", nullable = false, length = 10)
    @Enumerated(EnumType.STRING)
    val bindingState: BindingState,

    @Column(name = "created_at", nullable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime? = null

) {
    enum class BindingState {
        PAUSE, RESUME
    }
}