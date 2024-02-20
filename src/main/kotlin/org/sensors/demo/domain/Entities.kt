package org.sensors.demo.domain

import jakarta.persistence.Convert
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.Id
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import org.sensors.demo.component.IntListConverter
import java.time.Instant
import java.util.UUID

@Entity
data class Sensor(
    val threshold: Int,
    val createdAt: Instant = Instant.now(),
    var lastWindowProcessed: Instant? = null,
    @Enumerated(EnumType.STRING)
    var status: SensorStatus = SensorStatus.OK,
    @Id @GeneratedValue
    val id: UUID? = null
)

@Entity
data class Measurement(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    val sensor: Sensor,
    val value: Int,
    val timestamp: Instant,
    @Id @GeneratedValue
    val id: UUID? = null
)

@Entity
data class Alert(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sensor_id")
    val sensor: Sensor,
    val startTime: Instant,
    val endTime: Instant,
    @Convert(converter = IntListConverter::class)
    val measurements: List<Int>,
    val timestamp: Instant = Instant.now(),
    @Id @GeneratedValue
    val id: UUID? = null
)

interface MeasurementStatsProjection {
    fun getMaxValue(): Int
    fun getAvgValue(): Int
}

enum class SensorStatus {
    OK, WARN, ALERT
}