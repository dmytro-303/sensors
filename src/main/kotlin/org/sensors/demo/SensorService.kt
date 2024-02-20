package org.sensors.demo

import org.sensors.demo.domain.Alert
import org.sensors.demo.domain.Measurement
import org.sensors.demo.domain.Sensor
import org.sensors.demo.domain.SensorNotFoundException
import org.sensors.demo.domain.SensorStatus
import org.sensors.demo.domain.SensorStatus.ALERT
import org.sensors.demo.domain.SensorStatus.OK
import org.sensors.demo.domain.SensorStatus.WARN
import org.sensors.demo.domain.repository.AlertRepository
import org.sensors.demo.domain.repository.MeasurementRepository
import org.sensors.demo.domain.repository.SensorRepository
import org.sensors.demo.dto.AlertResponse
import org.sensors.demo.dto.CreateSensorRequest
import org.sensors.demo.dto.CreateSensorResponse
import org.sensors.demo.dto.SensorMeasurementRequest
import org.sensors.demo.dto.SensorMetricsResponse
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class SensorService(
    private val sensorRepository: SensorRepository,
    private val measurementRepository: MeasurementRepository,
    private val alertRepository: AlertRepository,
    private val transactionTemplate: TransactionTemplate,
    @Value("\${sensors.threshold}")
    private val threshold: Int,
    @Value("\${sensors.metrics-days-count}")
    private val metricsDaysCount: Long
) {
    private val logger = LoggerFactory.getLogger(SensorService::class.java)

    fun getSensorById(id: UUID): Sensor =
        sensorRepository.findById(id).orElseThrow {
            throw SensorNotFoundException("Sensor ID $id is not found")
        }

    fun createSensor(request: CreateSensorRequest): CreateSensorResponse =
        sensorRepository.save(
            Sensor(threshold = request.threshold ?: threshold)
        ).let {
            logger.info("Created sensor ID ${it.id}")
            CreateSensorResponse(it.id!!)
        }

    // in order to provide fast response this method just saves measurements
    // and all the processing logic is performed later by scheduler
    fun saveMeasurement(sensorId: UUID, request: SensorMeasurementRequest) {
        measurementRepository.save(
            Measurement(
                getSensorById(sensorId), request.value, request.timestamp
            )
        )
    }

    // for test purposes the scheduler is set to 5 sec
    // but for the real world scenario it could be set to 1 minute (measurements interval)
    @Scheduled(fixedRate = 5000)
    fun processSensors() {
        logger.info("Start processing measurements")
        sensorRepository.findAll()
            .forEach { sensor ->
                transactionTemplate.execute { sensor.processMeasurements() }
            }
    }

    fun getSensorMetrics(sensorId: UUID): SensorMetricsResponse =
        measurementRepository.findMeasurementStats(
            sensorId, Instant.now().minus(Duration.ofDays(metricsDaysCount))
        ).let {
            SensorMetricsResponse(it.getMaxValue(), it.getAvgValue())
        }


    fun findSensorAlerts(uuid: UUID): List<AlertResponse> =
        alertRepository.findBySensorId(uuid)
            .map {
                AlertResponse(
                    startTime = it.startTime,
                    endTime = it.endTime,
                    measurement1 = it.measurements[0],
                    measurement2 = it.measurements[1],
                    measurement3 = it.measurements[2]
                )
            }

    private fun Sensor.processMeasurements() {
        // retrieve measurements taken after the last reading and aggregate them
        logger.debug("Start retrieving measurements")
        val measurements =
            measurementRepository.findMeasurementsCreatedAfter(id!!, lastWindowProcessed ?: createdAt)

        if (measurements.isEmpty()) {
            logger.warn("No measurements found for sensor ID ${this.id}")
            return
        }

        measurements
            .asSequence()
            .chunked(MEASUREMENT_WINDOW)
            .mapNotNull { measurementsChunk ->
                val lastMeasurementTime = measurementsChunk.last().timestamp
                val newStatus = determineStatus(measurementsChunk, status)

                // create alert if applicable
                val alert = createAlertOrNull(newStatus, measurementsChunk)

                // update sensor's timestamp and status
                status = newStatus
                if (measurementsChunk.size == MEASUREMENT_WINDOW)
                    lastWindowProcessed = lastMeasurementTime

                alert
            }.toList().also {
                // save entities
                sensorRepository.save(this)
                if (it.isNotEmpty()) alertRepository.saveAll(it)
                logger.info("Created ${it.size} alerts for sensor ID ${this.id}")
            }
    }

    private fun Sensor.createAlertOrNull(
        newStatus: SensorStatus,
        measurementsChunk: List<Measurement>
    ) = if (status != ALERT && newStatus == ALERT)
        Alert(
            sensor = this,
            startTime = measurementsChunk.first().timestamp,
            endTime = measurementsChunk.last().timestamp,
            measurementsChunk.map { it.value }
        )
    else null

    private fun determineStatus(
        measurements: List<Measurement>,
        status: SensorStatus
    ): SensorStatus {
        val highReadingsCount = measurements.count { it.value >= threshold }
        return when {
            highReadingsCount == MEASUREMENT_WINDOW && status != ALERT -> ALERT
            highReadingsCount == 0 -> OK
            highReadingsCount > 0 && status != ALERT && status != WARN -> WARN
            else -> status
        }
    }

    companion object {
        const val MEASUREMENT_WINDOW = 3
    }
}