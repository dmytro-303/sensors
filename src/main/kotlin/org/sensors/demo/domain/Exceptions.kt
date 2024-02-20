package org.sensors.demo.domain


data class SensorNotFoundException(override val message: String) : IllegalArgumentException()