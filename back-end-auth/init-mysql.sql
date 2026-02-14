-- ============================================
-- Script: Initialize MySQL for Auth Service
-- Description: Grant privileges to the spring user
-- ============================================

-- Grant all privileges on the database to the spring user
GRANT ALL PRIVILEGES ON your_name_database.* TO 'springuser'@'%';
FLUSH PRIVILEGES;
