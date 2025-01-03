FROM openjdk:17-alpine

WORKDIR /test

COPY src/p2p /test/p2p
COPY src/network /test/network
COPY src/gui /test/gui
COPY src/main /test/main
COPY test/shared /test/shared

ENV DOCKER_BOOL=1

RUN javac -d /test p2p/*.java network/*.java gui/*.java main/Main.java

RUN chmod -R 777 /test

EXPOSE 4113

CMD ["java", "-cp", "/test", "main.Main"]
