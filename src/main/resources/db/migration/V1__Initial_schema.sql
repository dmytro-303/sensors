CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE sensor
(
    id                    UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    threshold             INTEGER                  NOT NULL,
    created_at            TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    last_window_processed TIMESTAMP WITH TIME ZONE,
    status                VARCHAR(255)             NOT NULL
);

CREATE TABLE measurement
(
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    sensor_id UUID                     NOT NULL,
    value     INTEGER                  NOT NULL,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    FOREIGN KEY (sensor_id) REFERENCES sensor (id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS alert
(
    id           UUID PRIMARY KEY                  DEFAULT uuid_generate_v4(),
    sensor_id    UUID                     NOT NULL,
    start_time   TIMESTAMP WITH TIME ZONE NOT NULL,
    end_time     TIMESTAMP WITH TIME ZONE NOT NULL,
    measurements INTEGER[]                NOT NULL,
    timestamp    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    FOREIGN KEY (sensor_id) REFERENCES sensor (id) ON DELETE CASCADE
);