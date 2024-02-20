package org.sensors.demo

import PostgresContainer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.sensors.demo.domain.Alert
import org.sensors.demo.domain.Measurement
import org.sensors.demo.domain.Sensor
import org.sensors.demo.domain.SensorStatus.ALERT
import org.sensors.demo.domain.SensorStatus.OK
import org.sensors.demo.domain.SensorStatus.WARN
import org.sensors.demo.domain.repository.AlertRepository
import org.sensors.demo.domain.repository.MeasurementRepository
import org.sensors.demo.domain.repository.SensorRepository
import org.sensors.demo.dto.CreateSensorRequest
import org.sensors.demo.dto.SensorMeasurementRequest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.annotation.DirtiesContext
import org.springframework.test.context.TestPropertySource
import java.time.Duration
import java.time.Instant

@TestPropertySource(
    properties = [
        "threshold=2000",
        "metrics-days-count=30"
    ]
)
@SpringBootTest
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
class SensorServiceIntegrationTest {

    @Autowired
    private lateinit var sensorService: SensorService

    @Autowired
    private lateinit var sensorRepository: SensorRepository

    @Autowired
    private lateinit var measurementRepository: MeasurementRepository

    @Autowired
    private lateinit var alertRepository: AlertRepository

    init {
        PostgresContainer.instance // start postgres test container
    }

    @BeforeEach
    fun cleanup() {
        measurementRepository.deleteAll()
        alertRepository.deleteAll()
        sensorRepository.deleteAll()
    }

    @Test
    fun `should create sensor`() {
        // given
        val sensorRequest = CreateSensorRequest(threshold = 2000)

        // when
        sensorService.createSensor(sensorRequest)

        // then
        with(sensorRepository.findAll().single()) {
            assertEquals(sensorRequest.threshold, threshold)
        }
    }

    @Test
    fun `should create sensor with default setting `() {
        // given
        val sensorRequest = CreateSensorRequest()

        // when
        sensorService.createSensor(sensorRequest)

        // then
        // verify value from properties is used
        assertEquals(2000, sensorRepository.findAll().single().threshold)
    }

    @Test
    fun `should add sensor measurements`() {
        // given
        val sensor = sensorRepository.save(Sensor(2000))

        val measurementRequest = SensorMeasurementRequest(2000, Instant.now())
        // when

        sensorService.saveMeasurement(sensor.id!!, measurementRequest)

        // then
        with(measurementRepository.findAll().single()) {
            assertEquals(sensor, sensor)
            assertEquals(measurementRequest.value, value)
            assertEquals(measurementRequest.timestamp, timestamp)
        }
    }

    @Test
    fun `should process measurements and update sensor status from OK to WARN`() {
        // given
        val sensor = sensorRepository.save(Sensor(2000))
        val measurement = Measurement(sensor, 2001, Instant.now())
        measurementRepository.save(measurement)

        // when
        sensorService.processSensors()

        //then
        assertEquals(WARN, sensorRepository.findById(sensor.id!!).orElseThrow().status)
    }

    @Test
    fun `should process measurements and update sensor status from WARN to OK`() {
        // given
        val sensor = sensorRepository.save(Sensor(threshold = 2000, status = WARN))
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))

        // when
        sensorService.processSensors()

        //then
        assertEquals(OK, sensorRepository.findById(sensor.id!!).orElseThrow().status)
    }

    @Test
    fun `should process measurements and update sensor status FROM OK to ALERT`() {
        // given
        val sensor = sensorRepository.save(Sensor(2000))

        // 3 measurements made exceeding threshold
        val measurement1 = Measurement(sensor, 2100, Instant.now())
        val measurement2 = Measurement(sensor, 2200, Instant.now())
        val measurement3 = Measurement(sensor, 2300, Instant.now())

        measurementRepository.saveAll(listOf(measurement1, measurement2, measurement3))

        // when
        sensorService.processSensors()

        //then
        assertEquals(ALERT, sensorRepository.findById(sensor.id!!).orElseThrow().status)

        with(alertRepository.findBySensorId(sensor.id!!).single()) {
            assertEquals(measurement1.timestamp, startTime)
            assertEquals(measurement3.timestamp, endTime)
            assertEquals(listOf(measurement1.value, measurement2.value, measurement3.value), measurements)
        }
    }

    @Test
    fun `should process measurements and update sensor status FROM ALERT to OK`() {
        // given
        val sensor = sensorRepository.save(Sensor(threshold = 2000, status = ALERT))

        // 3 measurements made exceeding threshold
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))

        // when
        sensorService.processSensors()

        //then
        assertEquals(OK, sensorRepository.findById(sensor.id!!).orElseThrow().status)
    }

    @Test
    fun `should process measurements and not update sensor status`() {
        // given
        val sensor = sensorRepository.save(Sensor(2000))

        // 3 measurements below threshold
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))

        // when
        sensorService.processSensors()

        //then
        assertEquals(OK, sensorRepository.findById(sensor.id!!).orElseThrow().status)
    }

    @Test
    fun `should process measurements and not update sensor status to OK`() {
        // given
        val sensor = sensorRepository.save(Sensor(threshold = 2000, status = ALERT))

        // 3 measurements made but only 2 are below threshold
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))
        measurementRepository.save(Measurement(sensor, 2001, Instant.now()))
        measurementRepository.save(Measurement(sensor, 1999, Instant.now()))

        // when
        sensorService.processSensors()

        //then
        assertEquals(ALERT, sensorRepository.findById(sensor.id!!).orElseThrow().status)
    }

    @Test
    fun `should get sensor by id`() {
        // given
        val sensor = sensorRepository.save(Sensor(threshold = 2000, status = ALERT))

        // when
        val result = sensorService.getSensorById(sensor.id!!)

        //then
        assertEquals(sensor.id, result.id)
        assertEquals(sensor.threshold, result.threshold)
        assertEquals(sensor.createdAt, result.createdAt)
        assertEquals(sensor.status, result.status)
        assertEquals(sensor.lastWindowProcessed, result.lastWindowProcessed)
    }

    @Test
    fun `should get sensor metrics`() {
        // given
        val sensor = sensorRepository.save(Sensor(threshold = 2000, status = ALERT))

        val measurement1 = Measurement(sensor, 2000, Instant.now())
        val measurement2 = Measurement(sensor, 1990, Instant.now())
        // should not be included because it is before metrics-days-count
        val measurement3 = Measurement(sensor, 2001, Instant.now().minus(Duration.ofDays(30)))

        measurementRepository.saveAll(listOf(measurement1, measurement2, measurement3))

        // when
        val result = sensorService.getSensorMetrics(sensor.id!!)

        //then
        assertEquals(1995, result.avgLast30Days)
        assertEquals(measurement1.value, result.maxLast30Days)
    }


    @Test
    fun `should get sensor alerts`() {
        // given
        val sensor = sensorRepository.save(Sensor(threshold = 2000, status = ALERT))


        val alert = Alert(sensor, Instant.now(), Instant.now().plusSeconds(60), listOf(2010, 2020, 2030))

        alertRepository.save(alert)

        // when
        val result = sensorService.findSensorAlerts(sensor.id!!).single()

        //then
        assertEquals(alert.measurements[0], result.measurement1)
        assertEquals(alert.measurements[1], result.measurement2)
        assertEquals(alert.measurements[2], result.measurement3)
        assertEquals(alert.startTime, result.startTime)
        assertEquals(alert.endTime, result.endTime)
    }
}