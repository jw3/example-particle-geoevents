-- Database: events

CREATE DATABASE events
  WITH OWNER = postgres
       ENCODING = 'SQL_ASCII'
       TABLESPACE = pg_default
       LC_COLLATE = 'C'
       LC_CTYPE = 'C'
       CONNECTION LIMIT = -1;

\connect events;

CREATE EXTENSION postgis;
