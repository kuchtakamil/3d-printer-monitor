FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.8.2_2.13.10
WORKDIR /sensor-simulator
COPY . /sensor-simulator
CMD sbt "runMain DeviceSimulatorProducer ${SIMULATOR_TYPE}" && tail -f /dev/null
#"project ${PROJECT}"