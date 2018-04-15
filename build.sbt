name := "example-particle-geoserver"

version := "0.1"
organization := "com.github.jw3"
scalaVersion := "2.12.5"

libraryDependencies ++= {
  Seq(
    "com.typesafe.akka" %% "akka-http" % "10.1.1",
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % Test
  )
}
