geoevent server for the particle iot platform
===

A prototype geoevent and geofencing server that deploys to OpenShift online and is based on Akka, PostGis, and the Particle cloud platform.

### concepts
- Device: a location reporting component
- Feature: a geographic feature that
- Fence: a geometry with a callback

### fencing
- Features can be fenced, but are not always
- Fencing is a dynamic operation, Features are persistent

### Development

#### tools
- https://github.com/vi/websocat

#### geotrellis-vector (published to 2.12.x)

https://github.com/jw3/geotrellis/tree/2.12.5

- Provides a scala idiomatic wrapper around JTS types: Point, Line (LineString in JTS), Polygon, MultiPoint, MultiLine (MultiLineString in JTS), MultiPolygon, GeometryCollection
- Methods for geometric operations supported in JTS, with results that provide a type-safe way to match over possible results of geometries.
- Provides a Feature type that is the composition of a geometry and a generic data type.
- Read and write geometries and features to and from GeoJSON.
- Read and write geometries to and from WKT and WKB.
- Reproject geometries between two CRSs.
- Geometric operations: Convex Hull, Densification, Simplification
- Perform Kriging interpolation on point values.
- Perform affine transformations of geometries


#### particle webhook format

```
{
  "id": "{{{PARTICLE_DEVICE_ID}}}",
  "event": "{{{PARTICLE_EVENT_NAME}}}",
  "data": "{{{PARTICLE_EVENT_VALUE}}}",
  "when": "{{{PARTICLE_PUBLISHED_AT}}}"
}
```

#### event names
- `up` device online
- `dn` device offline
- `mv` device moved

#### configuration

Most settings are exposed in the environment

- `GEO_PERSIST`: enable jdbc connection
- `GEO_HTTP_PORT`: port to serve http on
- `ACTOR_LOG_LEVEL`: level to log actors

