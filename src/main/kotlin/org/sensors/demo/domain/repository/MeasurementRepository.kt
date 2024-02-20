package org.sensors.demo.domain.repository

import org.sensors.demo.domain.Measurement
import org.sensors.demo.domain.MeasurementStatsProjection
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.CrudRepository
import java.time.Instant
import java.util.UUID

interface MeasurementRepository : CrudRepository<Measurement, UUID> {

    @Query("SELECT m FROM Measurement m WHERE m.sensor.id = :sensorId AND m.timestamp > :createdAfter ORDER BY m.timestamp ASC")
    fun findMeasurementsCreatedAfter(
        sensorId: UUID,
        createdAfter: Instant
    ): List<Measurement>

    @Query(
        "SELECT MAX(m.value) as maxValue, AVG(m.value) as avgValue " +
                "FROM Measurement m WHERE m.sensor.id = :sensorId AND m.timestamp > :createdAfter"
    )
    fun findMeasurementStats(
        sensorId: UUID,
        createdAfter: Instant
    ): MeasurementStatsProjection

}