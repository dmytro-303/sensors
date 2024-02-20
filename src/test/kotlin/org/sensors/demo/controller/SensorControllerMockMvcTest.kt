package org.sensors.demo.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.ninjasquad.springmockk.MockkBean
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import org.junit.jupiter.api.Test
import org.sensors.demo.SensorService
import org.sensors.demo.domain.Sensor
import org.sensors.demo.domain.SensorNotFoundException
import org.sensors.demo.domain.SensorStatus
import org.sensors.demo.dto.AlertResponse
import org.sensors.demo.dto.CreateSensorRequest
import org.sensors.demo.dto.CreateSensorResponse
import org.sensors.demo.dto.SensorMeasurementRequest
import org.sensors.demo.dto.SensorMetricsResponse
import org.sensors.demo.dto.SensorStatusResponse
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.content
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.time.Instant
import java.util.UUID

@WebMvcTest(SensorController::class)
class SensorControllerMockMvcTest {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockkBean
    private lateinit var sensorService: SensorService

    @Test
    fun `should create sensor`() {
        val sensorId = UUID.randomUUID()
        val request = CreateSensorRequest(threshold = 100)
        val response = CreateSensorResponse(sensorId = sensorId)

        every { sensorService.createSensor(any()) } returns response

        mockMvc.perform(
            post("/api/v1/sensors")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)
    }

    @Test
    fun `should collect measurements`() {
        val sensorId = UUID.randomUUID()
        val request = SensorMeasurementRequest(value = 200, timestamp = Instant.now())

        every { sensorService.saveMeasurement(sensorId, request) } just Runs

        mockMvc.perform(
            post("/api/v1/sensors/$sensorId/measurements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isAccepted)
    }

    @Test
    fun `test get sensor status`() {
        val sensorId = UUID.randomUUID()
        val sensorStatusResponse = SensorStatusResponse(SensorStatus.OK)

        every { sensorService.getSensorById(sensorId) } returns Sensor(2000, status = SensorStatus.OK)

        mockMvc.perform(get("/api/v1/sensors/$sensorId"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.status").value(sensorStatusResponse.status.name))
    }

    @Test
    fun `test get sensor metrics`() {
        val sensorId = UUID.randomUUID()
        val sensorMetricsResponse = SensorMetricsResponse(maxLast30Days = 1200, avgLast30Days = 900)

        every { sensorService.getSensorMetrics(sensorId) } returns sensorMetricsResponse

        mockMvc.perform(get("/api/v1/sensors/$sensorId/metrics"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.maxLast30Days").value(sensorMetricsResponse.maxLast30Days))
            .andExpect(jsonPath("$.avgLast30Days").value(sensorMetricsResponse.avgLast30Days))
    }

    @Test
    fun `test find sensor alerts`() {
        val sensorId = UUID.randomUUID()
        val alerts = listOf(
            AlertResponse(Instant.now(), Instant.now().plusSeconds(3600), 100, 150, 200)
        )

        every { sensorService.findSensorAlerts(sensorId) } returns alerts

        mockMvc.perform(get("/api/v1/sensors/$sensorId/alerts"))
            .andExpect(status().isOk)
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$[0].measurement1").value(alerts[0].measurement1))
            .andExpect(jsonPath("$[0].measurement2").value(alerts[0].measurement2))
            .andExpect(jsonPath("$[0].measurement3").value(alerts[0].measurement3))
    }

    @Test
    fun `should handle client exception`() {
        val sensorId = UUID.randomUUID()
        val message = "message"
        every { sensorService.findSensorAlerts(sensorId) } throws SensorNotFoundException(message)

        // Perform request that would trigger the exception
        mockMvc.perform(get("/api/v1/sensors/$sensorId/alerts"))
            .andExpect(status().isBadRequest)
            .andExpect(content().string(message))
    }

    @Test
    fun `should handle server exception`() {
        val sensorId = UUID.randomUUID()
        every { sensorService.findSensorAlerts(sensorId) } throws RuntimeException()

        // Perform request that would trigger the exception
        mockMvc.perform(get("/api/v1/sensors/$sensorId/alerts"))
            .andExpect(status().is5xxServerError)
    }
}