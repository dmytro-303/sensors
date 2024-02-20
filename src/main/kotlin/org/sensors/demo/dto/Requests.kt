package org.sensors.demo.dto

import java.time.Instant


data class CreateSensorRequest(
    val threshold: Int? = null
)

data class SensorMeasurementRequest(
    val value: Int,
    val timestamp: Instant
)

