CREATE TABLE fences (
  id SERIAL PRIMARY KEY,
  name VARCHAR(255),
  geometry GEOMETRY(Polygon, 0)
);
