-- Administrator-only script.
-- The PowerShell bootstrap script performs the same check before calling MySQL.
-- This file never drops the database and never changes privileges of other databases.

CREATE DATABASE IF NOT EXISTS anime_platform
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_0900_ai_ci;

USE anime_platform;

