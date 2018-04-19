example geoserver for particle devices
===


### objects
- Device: a location reporting component
- Feature: a geographic feature that
- Fence: a geometry with a callback

### fencing
- Features can be fenced, but are not always
- Fencing is a dynamic operation, Features are persistent


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
