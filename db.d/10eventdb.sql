-- Database: events

CREATE DATABASE geoevents
  WITH OWNER = postgres
       ENCODING = 'SQL_ASCII'
       TABLESPACE = pg_default
       LC_COLLATE = 'C'
       LC_CTYPE = 'C'
       CONNECTION LIMIT = -1;

\c geoevents;

CREATE EXTENSION postgis;
