enablePlugins(DockerComposePlugin)

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "1.0"
lazy val root =
  project.in(file("."))
    .aggregate(model, `sensor-simulator`, consumer)


lazy val model =
project.in(file("model"))

lazy val consumer =
  project.in(file("consumer"))
    .dependsOn(model)
    .settings(commonSettings)

lazy val `sensor-simulator` =
project.in(file("sensor-simulator"))
  .dependsOn(model)
  .settings(commonSettings)

val commonSettings =
  Seq(
    libraryDependencies ++= Seq(
      "com.evolutiongaming" %% "skafka" % "15.0.0",
      "org.apache.kafka" % "kafka-clients" % "3.4.0",
      "org.apache.kafka" % "kafka-streams" % "3.4.0",
      "org.apache.kafka" %% "kafka-streams-scala" % "3.4.0",
      "com.github.pureconfig" %% "pureconfig" % "0.17.2",
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5",
      "org.http4s" %% "http4s-ember-client" % "0.23.18",
      "org.http4s" %% "http4s-ember-server" % "0.23.18",
      "org.http4s" %% "http4s-dsl" % "0.23.18",
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )

