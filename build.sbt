name := "example-particle-geoserver"

version := "0.1"
organization := "com.github.jw3"
scalaVersion := "2.12.5"

libraryDependencies ++= {
  Seq(
    // misc
    "com.iheart" %% "ficus" % "1.4.3",
    // akka
    "com.typesafe.akka" %% "akka-actor" % "2.5.12",
    "com.typesafe.akka" %% "akka-stream" % "2.5.12",
    "com.typesafe.akka" %% "akka-http" % "10.1.1",
    // logging
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    // testing
    "com.typesafe.akka" %% "akka-testkit" % "2.5.12" % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % Test
  )
}
