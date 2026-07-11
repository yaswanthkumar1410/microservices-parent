-- Runs once on first MySQL container init (empty data dir).
-- Creates the schemas order-service and inventory-service expect.
CREATE DATABASE IF NOT EXISTS `order-service`;
CREATE DATABASE IF NOT EXISTS `inventory-service`;
