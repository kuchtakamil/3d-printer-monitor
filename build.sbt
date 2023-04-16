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
    .settings(consumerDeps, commonDeps)

lazy val `sensor-simulator` =
project.in(file("sensor-simulator"))
  .dependsOn(model)
  .settings(producerDeps, commonDeps)

lazy val skafka = "15.0.0"
lazy val kafkaFlow = "2.3.1"
lazy val http4 = "0.23.18"
lazy val circe = "0.14.5"
lazy val pureconfig = "0.17.2"

val commonDeps =
  Seq(
    libraryDependencies ++= Seq(
      "com.evolutiongaming" %% "skafka" % skafka,
      "com.github.pureconfig" %% "pureconfig" % pureconfig,
      "io.circe" %% "circe-core" % circe,
      "io.circe" %% "circe-generic" % circe,
      "io.circe" %% "circe-parser" % circe,
    ),
    testFrameworks += new TestFramework("munit.Framework")
  )
val producerDeps =
  Seq(
    libraryDependencies ++= Seq(
      "com.evolutiongaming" %% "skafka" % skafka,
    )
)
val consumerDeps =
Seq(
  libraryDependencies ++= Seq(
    "com.evolutiongaming" %% "skafka" % skafka,
    "com.github.pureconfig" %% "pureconfig" % pureconfig,
    "org.http4s" %% "http4s-ember-client" % http4,
    "org.http4s" %% "http4s-ember-server" % http4,
    "org.http4s" %% "http4s-dsl" % http4,
    )
  )
