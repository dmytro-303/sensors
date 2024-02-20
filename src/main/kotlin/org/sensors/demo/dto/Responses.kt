package org.sensors.demo.dto

import org.sensors.demo.domain.SensorStatus
import java.time.Instant
import java.util.UUID

data class AlertResponse(
    val startTime: Instant,
    val endTime: Instant,
    val measurement1: Int,
    val measurement2: Int,
    val measurement3: Int
)

data class CreateSensorResponse(
    val sensorId: UUID
)

data class SensorStatusResponse(
    val status: SensorStatus
)

data class SensorMetricsResponse(val maxLast30Days: Int, val avgLast30Days: Int)
