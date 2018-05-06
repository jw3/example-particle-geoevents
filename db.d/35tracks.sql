CREATE TABLE tracks (
  id SERIAL PRIMARY KEY,
  device VARCHAR(128) NOT NULL,
  starting_offset BIGINT NOT NULL,
  geometry GEOMETRY(Linestring, 0) NOT NULL
);
