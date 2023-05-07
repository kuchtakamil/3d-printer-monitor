FROM sbtscala/scala-sbt:eclipse-temurin-jammy-17.0.5_8_1.8.2_2.13.10
WORKDIR /data-consumer
COPY . /data-consumer
CMD sbt "project ${PROJECT}" "runMain KafkaConsumer ${SIMULATOR_TYPE}" && tail -f /dev/null