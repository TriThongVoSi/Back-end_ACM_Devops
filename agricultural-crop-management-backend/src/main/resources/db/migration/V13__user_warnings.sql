ALTER TABLE `users`
  ADD COLUMN `locked_until` DATETIME NULL AFTER `status`;

CREATE TABLE IF NOT EXISTS `user_warnings` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT NOT NULL,
  `created_by` BIGINT,
  `decision` VARCHAR(30) NOT NULL,
  `description` TEXT,
  `created_at` DATETIME,
  `lock_until` DATETIME,
  PRIMARY KEY (`id`),
  KEY `idx_user_warnings_user_id` (`user_id`),
  KEY `idx_user_warnings_created_by` (`created_by`),
  CONSTRAINT `fk_user_warnings_user`
    FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
  CONSTRAINT `fk_user_warnings_created_by`
    FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
