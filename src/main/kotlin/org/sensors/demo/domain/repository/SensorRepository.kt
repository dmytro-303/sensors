package org.sensors.demo.domain.repository

import org.sensors.demo.domain.Sensor
import org.springframework.data.repository.CrudRepository
import java.util.UUID

interface SensorRepository : CrudRepository<Sensor, UUID> {
}