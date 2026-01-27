-- =========================================================
-- V9: Create user preferences table
-- =========================================================
-- Stores per-user currency, weight unit, and locale preferences.
-- =========================================================

CREATE TABLE IF NOT EXISTS `user_preferences` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `currency_code` VARCHAR(10) NOT NULL DEFAULT 'VND',
  `weight_unit` VARCHAR(10) NOT NULL DEFAULT 'KG',
  `locale` VARCHAR(20) DEFAULT 'vi-VN',
  `created_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  `updated_at` TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_preferences_user` (`user_id`),
  CONSTRAINT `fk_user_preferences_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
