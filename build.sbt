scalaVersion := "2.13.8"

lazy val model = project

val `3d-printer-monitor` = project
  .in(file("."))
  .aggregate(
    model
  )
  .settings(
    name := "3d-printer-monitor",
    libraryDependencies ++= Seq(
      "org.apache.kafka" % "kafka-clients" % "3.4.0",
      "org.apache.kafka" % "kafka-streams" % "3.4.0",
      "org.apache.kafka" %% "kafka-streams-scala" % "3.4.0",
      "io.circe" %% "circe-core" % "0.14.5",
      "io.circe" %% "circe-generic" % "0.14.5",
      "io.circe" %% "circe-parser" % "0.14.5"
    )
  )

