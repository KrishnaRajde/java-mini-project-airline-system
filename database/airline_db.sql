-- Airline Reservation Management System
-- Import this file in phpMyAdmin (XAMPP) -> Import tab -> choose this file -> Go

CREATE DATABASE IF NOT EXISTS airline_db;
USE airline_db;

DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS bookings;
DROP TABLE IF EXISTS seats;
DROP TABLE IF EXISTS flights;
DROP TABLE IF EXISTS customers;
DROP TABLE IF EXISTS admins;

CREATE TABLE admins (
    admin_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL
) ENGINE=InnoDB;

CREATE TABLE customers (
    customer_id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    email VARCHAR(100) NOT NULL UNIQUE,
    phone VARCHAR(15) NOT NULL,
    password VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE TABLE flights (
    flight_id INT AUTO_INCREMENT PRIMARY KEY,
    flight_no VARCHAR(10) NOT NULL,
    airline VARCHAR(50) NOT NULL,
    source VARCHAR(50) NOT NULL,
    destination VARCHAR(50) NOT NULL,
    departure_time DATETIME NOT NULL,
    arrival_time DATETIME NOT NULL,
    price DECIMAL(10,2) NOT NULL,
    total_seats INT NOT NULL,
    available_seats INT NOT NULL,
    UNIQUE (flight_no, departure_time),
    INDEX idx_route (source, destination, departure_time),
    CHECK (available_seats >= 0 AND available_seats <= total_seats)
) ENGINE=InnoDB;

CREATE TABLE seats (
    seat_id INT AUTO_INCREMENT PRIMARY KEY,
    flight_id INT NOT NULL,
    seat_no VARCHAR(5) NOT NULL,
    status ENUM('AVAILABLE', 'BOOKED') NOT NULL DEFAULT 'AVAILABLE',
    UNIQUE (flight_id, seat_no),
    FOREIGN KEY (flight_id) REFERENCES flights(flight_id) ON DELETE CASCADE
) ENGINE=InnoDB;

CREATE TABLE bookings (
    booking_id INT AUTO_INCREMENT PRIMARY KEY,
    pnr VARCHAR(10) NOT NULL,              -- same PNR for all passengers of one booking
    customer_id INT NOT NULL,
    flight_id INT NOT NULL,
    seat_id INT NOT NULL,
    passenger_name VARCHAR(100) NOT NULL,
    passenger_age INT NOT NULL,
    passenger_gender VARCHAR(10) NOT NULL,
    booking_date DATETIME DEFAULT CURRENT_TIMESTAMP,
    status ENUM('CONFIRMED', 'CANCELLED') NOT NULL DEFAULT 'CONFIRMED',
    ticket_price DECIMAL(10,2) NOT NULL,
    INDEX idx_customer (customer_id),
    INDEX idx_pnr (pnr),
    FOREIGN KEY (customer_id) REFERENCES customers(customer_id),
    FOREIGN KEY (flight_id) REFERENCES flights(flight_id) ON DELETE CASCADE,
    FOREIGN KEY (seat_id) REFERENCES seats(seat_id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- dummy QR payments (demo only, no real money)
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

-- ---------- sample data ----------

INSERT INTO admins (username, password) VALUES ('admin', 'admin123');

INSERT INTO customers (name, email, phone, password)
VALUES ('Rahul Sharma', 'rahul@gmail.com', '9876543210', '1234');

INSERT INTO flights (flight_no, airline, source, destination, departure_time, arrival_time, price, total_seats, available_seats) VALUES
('AI101', 'Air India', 'Mumbai', 'Delhi', '2026-12-10 08:00:00', '2026-12-10 10:15:00', 4500.00, 12, 12),
('6E202', 'IndiGo', 'Delhi', 'Mumbai', '2026-12-12 18:30:00', '2026-12-12 20:40:00', 3900.00, 12, 12),
('UK303', 'Vistara', 'Mumbai', 'Bangalore', '2026-12-15 06:45:00', '2026-12-15 08:30:00', 5200.00, 12, 12);

-- 12 seats for each sample flight (2 rows x 6 seats)
INSERT INTO seats (flight_id, seat_no) VALUES
(1,'1A'),(1,'1B'),(1,'1C'),(1,'1D'),(1,'1E'),(1,'1F'),(1,'2A'),(1,'2B'),(1,'2C'),(1,'2D'),(1,'2E'),(1,'2F'),
(2,'1A'),(2,'1B'),(2,'1C'),(2,'1D'),(2,'1E'),(2,'1F'),(2,'2A'),(2,'2B'),(2,'2C'),(2,'2D'),(2,'2E'),(2,'2F'),
(3,'1A'),(3,'1B'),(3,'1C'),(3,'1D'),(3,'1E'),(3,'1F'),(3,'2A'),(3,'2B'),(3,'2C'),(3,'2D'),(3,'2E'),(3,'2F');
