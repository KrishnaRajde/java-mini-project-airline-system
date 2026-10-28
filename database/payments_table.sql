-- run this once in phpMyAdmin (SQL tab) if your database already exists
-- (airline_db.sql already has this table for a fresh import)
USE airline_db;

CREATE TABLE IF NOT EXISTS payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,
    payment_ref VARCHAR(12) NOT NULL UNIQUE,      -- shown in QR code and website
    customer_id INT NOT NULL,
    flight_id INT NOT NULL,
    seats VARCHAR(100) NOT NULL,                  -- eg. 1A, 1B
    passengers INT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    status ENUM('PENDING', 'SUCCESS', 'FAILED') NOT NULL DEFAULT 'PENDING',
    pnr VARCHAR(10),                              -- filled after booking is confirmed
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (flight_id) REFERENCES flights(flight_id) ON DELETE CASCADE
) ENGINE=InnoDB;
