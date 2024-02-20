### Running the app
To run the app in Docker along with postgres you can use the following commands
```
./gradlew bootJar      
docker-compose up --build
```
### Note
The measurements are accepted and stored right away but further processing is done by scheduler which is set to 5 sec for testing purposes

### Requests
Create sensor with optional param threshold
```
POST /api/v1/sensors
{
  "threshold": 100
}
```
Send measurements
```
POST {{baseUrl}}/api/v1/sensors/{{sensorId}}/measurements
{
  "value": 200,
  "timestamp": "2024-03-01T12:00:00Z"
}
```
Get status
```
GET {{baseUrl}}/api/v1/sensors/{{sensorId}}
```
Get metrics
```
GET {{baseUrl}}/api/v1/sensors/{{sensorId}}/metrics
```
Get alerts
```
{{baseUrl}}/api/v1/sensors/{{sensorId}}/alerts
```
