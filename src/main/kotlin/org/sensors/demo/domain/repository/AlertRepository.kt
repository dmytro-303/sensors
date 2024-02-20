package org.sensors.demo.domain.repository

import org.sensors.demo.domain.Alert
import org.springframework.data.repository.CrudRepository
import org.springframework.stereotype.Repository
import java.util.UUID

@Repository
interface AlertRepository : CrudRepository<Alert, UUID> {

    fun findBySensorId(sensorId: UUID): List<Alert>
}