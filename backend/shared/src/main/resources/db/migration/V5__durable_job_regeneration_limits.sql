CREATE TABLE application_regeneration_usage (
    device_id UUID NOT NULL REFERENCES devices(id) ON DELETE CASCADE,
    job_id UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    usage_day DATE NOT NULL,
    regeneration_count INTEGER NOT NULL DEFAULT 0 CHECK (regeneration_count >= 0),
    PRIMARY KEY (device_id, job_id, usage_day)
);

INSERT INTO application_regeneration_usage(device_id, job_id, usage_day, regeneration_count)
SELECT device_id, job_id, regeneration_day, regenerations_today
FROM applications
WHERE regenerations_today > 0;
