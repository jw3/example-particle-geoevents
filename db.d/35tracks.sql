CREATE TABLE tracks (
  id SERIAL PRIMARY KEY,
  device VARCHAR(128) NOT NULL,
  startseq BIGINT NOT NULL,
  geometry GEOMETRY(Linestring, 0) NOT NULL
);
