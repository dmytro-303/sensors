package org.sensors.demo.controller

import org.sensors.demo.SensorService
import org.sensors.demo.dto.AlertResponse
import org.sensors.demo.dto.CreateSensorRequest
import org.sensors.demo.dto.CreateSensorResponse
import org.sensors.demo.dto.SensorMeasurementRequest
import org.sensors.demo.dto.SensorMetricsResponse
import org.sensors.demo.dto.SensorStatusResponse
import org.springframework.http.HttpStatus.ACCEPTED
import org.springframework.http.HttpStatus.CREATED
import org.springframework.http.HttpStatus.OK
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/v1/sensors")
class SensorController(private val sensorService: SensorService) {

    @PostMapping
    @ResponseStatus(CREATED)
    fun createSensor(@RequestBody request: CreateSensorRequest): CreateSensorResponse {
        return sensorService.createSensor(request)
    }

    @PostMapping("/{sensorId}/measurements")
    @ResponseStatus(ACCEPTED)
    fun collectMeasurement(
        @PathVariable sensorId: UUID,
        @RequestBody request: SensorMeasurementRequest
    ) {
        sensorService.saveMeasurement(sensorId, request)
    }

    @GetMapping("/{sensorId}", produces = [APPLICATION_JSON_VALUE])
    @ResponseStatus(OK)
    fun getSensorStatus(@PathVariable sensorId: UUID): SensorStatusResponse =
        SensorStatusResponse(sensorService.getSensorById(sensorId).status)

    @GetMapping("/{sensorId}/metrics", produces = [APPLICATION_JSON_VALUE])
    @ResponseStatus(OK)
    fun getSensorMetrics(@PathVariable sensorId: UUID): SensorMetricsResponse {
        return sensorService.getSensorMetrics(sensorId)
    }

    @GetMapping("/{uuid}/alerts", produces = [APPLICATION_JSON_VALUE])
    @ResponseStatus(OK)
    fun listSensorAlerts(@PathVariable uuid: UUID): List<AlertResponse> {
        return sensorService.findSensorAlerts(uuid)
    }
}
