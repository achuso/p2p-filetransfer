FROM openjdk:17-alpine

WORKDIR /test
COPY src/p2p /test/p2p
COPY src/network /test/network
COPY shared /test/shared

RUN javac -d /test p2p/*.java network/*.java
EXPOSE 4113 4114
CMD ["java", "-cp", "/test", "p2p.Node"]