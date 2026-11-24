CREATE TABLE IF NOT EXISTS spare_parts (
    id VARCHAR(64) PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    quantity INT NOT NULL CHECK (quantity >= 0)
);
CREATE TABLE IF NOT EXISTS repair_jobs (
    id VARCHAR(64) PRIMARY KEY,
    device VARCHAR(255) NOT NULL,
    issue TEXT NOT NULL
);
CREATE TABLE IF NOT EXISTS part_usage (
    usage_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id VARCHAR(64) NOT NULL,
    part_id VARCHAR(64) NOT NULL,
    quantity INT NOT NULL CHECK (quantity > 0),
    FOREIGN KEY (job_id) REFERENCES repair_jobs(id),
    FOREIGN KEY (part_id) REFERENCES spare_parts(id)
);
