enablePlugins(GitVersioning, BuildInfoPlugin, JavaServerAppPackaging, DockerPlugin)

name := "example-particle-geoserver"
organization := "com.github.jw3"
scalaVersion := "2.12.5"
git.useGitDescribe := true

buildInfoPackage := organization.value

scalacOptions ++= Seq(
  "-target:jvm-1.8",
  "-encoding",
  "UTF-8",
  "-feature",
  "-unchecked",
  "-deprecation",
  "-language:postfixOps",
  "-language:implicitConversions",
  "-Ywarn-unused-import",
  "-Xfatal-warnings",
  "-Xlint:_"
)

resolvers += Resolver.bintrayRepo("jw3", "maven")

libraryDependencies ++= {
  Seq(
    // misc
    "com.iheart" %% "ficus" % "1.4.3",
    "com.github.jw3" %% "geotrellis-vector" % "12.2.0.0",
    "com.github.jw3" %% "geotrellis-slick" % "12.2.0.0",
    // akka
    "com.typesafe.akka" %% "akka-actor" % "2.5.12",
    "com.typesafe.akka" %% "akka-stream" % "2.5.12",
    "com.typesafe.akka" %% "akka-http" % "10.1.1",
    "com.typesafe.akka" %% "akka-http-spray-json" % "10.1.1",
    "com.lightbend.akka" %% "akka-stream-alpakka-mqtt" % "1.1.2",
    // logging
    "com.typesafe.akka" %% "akka-slf4j" % "2.5.12",
    "ch.qos.logback" % "logback-classic" % "1.2.3",
    "com.typesafe.scala-logging" %% "scala-logging" % "3.9.0",
    // persistence
    "com.typesafe.slick" %% "slick" % "3.2.0",
    "org.postgresql" % "postgresql" % "42.2.2",
    "com.github.tminglei" %% "slick-pg" % "0.19.3",
    "com.github.dnvriend" %% "akka-persistence-jdbc" % "3.3.0",
    "com.github.dnvriend" %% "akka-persistence-inmemory" % "2.5.1.1",
    // test
    "com.typesafe.akka" %% "akka-testkit" % "2.5.12" % Test,
    "com.typesafe.akka" %% "akka-http-testkit" % "10.1.1" % Test,
    "org.scalactic" %% "scalactic" % "3.0.5" % Test,
    "org.scalatest" %% "scalatest" % "3.0.5" % Test
  )
}

dockerUpdateLatest := true
dockerExposedPorts := Seq(9000)
dockerBaseImage := "adoptopenjdk/openjdk11:debianslim-jre"
