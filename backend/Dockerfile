# Imagem da API para o deploy (ADR-0033). Duas etapas: o Maven gera o JAR; a imagem final só tem o JRE e o JAR.
# Gerar localmente: docker build -t pcp-backend ./backend

FROM maven:3.9-eclipse-temurin-26 AS build
WORKDIR /app
# O Checkstyle roda na fase validate e lê o checkstyle.xml; o pom vem antes do código para reaproveitar o cache das
# dependências quando só o código muda.
COPY pom.xml checkstyle.xml ./
RUN mvn -B -q dependency:go-offline
COPY src ./src
# Os testes rodam no CI (mvn -B verify), que precisa do Docker para o Testcontainers; aqui só se empacota.
RUN mvn -B -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S pcp && adduser -S -G pcp pcp
COPY --from=build --chown=pcp:pcp /app/target/pcp-backend-*.jar app.jar
USER pcp
# A JVM usa até 75% da memória do contêiner e encerra no OutOfMemoryError, para a plataforma reiniciar o serviço em
# vez de deixá-lo meio vivo.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
