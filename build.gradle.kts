plugins {
  id("org.springframework.boot") version "2.5.3"
  id("io.spring.dependency-management") version "1.0.11.RELEASE"
  id("java")
  id("com.gorylenko.gradle-git-properties") version "2.2.1"
  checkstyle
}

group = "com.nopadding.banana"
version = "1.0.0"

repositories {
  mavenCentral()
}

configure<JavaPluginConvention> {
  setSourceCompatibility(1.8)
  setTargetCompatibility(1.8)
}

dependencies {
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-data-redis")
  implementation("org.apache.camel.springboot:camel-spring-boot-starter:3.12.0")
  implementation("org.apache.camel.springboot:camel-syslog-starter:3.12.0")
  implementation("org.apache.camel.springboot:camel-kafka-starter:3.12.0")
  implementation("org.apache.camel.springboot:camel-avro-starter:3.12.0")

  implementation("org.projectlombok:lombok")
  implementation("com.h2database:h2")
  implementation("commons-codec:commons-codec:1.15")
  implementation("commons-io:commons-io:2.11.0")
  implementation("org.apache.commons:commons-lang3:3.12.0")

  developmentOnly("org.springframework.boot:spring-boot-devtools")

  annotationProcessor("org.projectlombok:lombok")
  testAnnotationProcessor("org.projectlombok:lombok")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
}

tasks.withType<Test> {
  useJUnitPlatform()
}

springBoot {
  buildInfo()
}

tasks.named<org.springframework.boot.gradle.tasks.run.BootRun>("bootRun") {
  environment("spring.profiles.active", "dev")
}

tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
  archiveFileName.set("dnsmasq-syslog.jar")
  launchScript()
}

checkstyle {
  configFile = file("config/checkstyle/google_checks.xml")
  dependencies {
    checkstyle("com.puppycrawl.tools:checkstyle:8.31")
    checkstyle("com.github.sevntu-checkstyle:sevntu-checks:1.37.1")
  }
}
