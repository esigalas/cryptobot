FROM openjdk:14-alpine

# Get gradle distribution
COPY *.gradle gradle.* gradlew /src/
COPY gradle /src/gradle
WORKDIR /src
RUN ./gradlew --version

# Copy over project
COPY . /src

CMD ["./gradlew", "run", "-i"]
