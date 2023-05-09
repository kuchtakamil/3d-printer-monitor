ThisBuild / scalaVersion := "2.13.10"
ThisBuild / version := "1.0"

lazy val root =
  project
    .in(file("."))
    .aggregate(model, `sensor-simulator`, `data-consumer`)

lazy val model =
  project.in(file("model"))

lazy val `data-consumer` =
  project
    .in(file("data-consumer"))
    .dependsOn(model)
    .settings(
      Compile / mainClass := Some("KafkaConsumer")
    )
    .settings(consumerDeps, commonDeps)

lazy val `sensor-simulator` =
  project
    .in(file("sensor-simulator"))
    .dependsOn(model)
    .settings(
      Compile / mainClass := Some("DeviceSimulatorProducer")
    )
    .settings(producerDeps, commonDeps)

lazy val skafkaVer     = "15.0.0"
lazy val kafkaFlowVer  = "2.3.1"
lazy val http4sVer     = "0.23.18"
lazy val circeVer      = "0.14.5"
lazy val pureconfigVer = "0.17.2"
lazy val fs2Ver        = "3.6.1"

val commonDeps   =
  Seq(
    libraryDependencies ++= Seq(
      "com.evolutiongaming"   %% "skafka"        % skafkaVer,
      "com.github.pureconfig" %% "pureconfig"    % pureconfigVer,
      "io.circe"              %% "circe-core"    % circeVer,
      "io.circe"              %% "circe-generic" % circeVer,
      "io.circe"              %% "circe-parser"  % circeVer,
    ),
    testFrameworks += new TestFramework("munit.Framework"),
  )
val producerDeps =
  Seq(
    libraryDependencies ++= Seq(
      "com.evolutiongaming" %% "skafka"   % skafkaVer,
      "co.fs2"              %% "fs2-core" % fs2Ver,
    )
  )

val consumerDeps =
  Seq(
    libraryDependencies ++= Seq(
      "com.evolutiongaming"   %% "skafka"              % skafkaVer,
      "com.github.pureconfig" %% "pureconfig"          % pureconfigVer,
      "org.http4s"            %% "http4s-ember-client" % http4sVer,
      "org.http4s"            %% "http4s-ember-server" % http4sVer,
      "org.http4s"            %% "http4s-dsl"          % http4sVer,
      "org.http4s"            %% "http4s-circe"        % http4sVer,
    )
  )
