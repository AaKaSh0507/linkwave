-- =============================================================================
-- PostgreSQL Initialization Script
-- Creates database, user, and applies initial schema
-- =============================================================================

-- Create extensions
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- Performance settings for the session
SET statement_timeout = '60s';

-- Grant permissions
GRANT ALL PRIVILEGES ON DATABASE linkwave TO linkwave;

-- Create indexes for common queries (will be created by JPA hibernate, but adding for reference)
-- The actual schema is managed by Spring Boot JPA hibernate auto-ddl

\echo 'Database initialization complete!'
