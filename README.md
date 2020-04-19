# spring-security-basics [![CI](https://github.com/daggerok/spring-security-basics/workflows/CI/badge.svg)](https://github.com/daggerok/spring-security-basics/actions?query=workflow%3ACI)
Learn Spring Security by baby steps from zero to pro!

## Table of Content
* [Step 0: No security](#step-0)
* [Versioning and releasing](#maven)
* [Resources and used links](#resources)

## step: 0

let's use simple spring boot web app

### application

use needed dependencies in `pom.xml` file:

```xml
  <dependencies>
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-web</artifactId>
    </dependency>
  </dependencies>
```

add in `App.java` file controller for index page:

```java
@Controller
class IndexPage {

  @GetMapping("/")
  String index() {
    return "index.html";
  }
}
```

do not forget about `src/main/resources/static/index.html` template file:

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="ie=edge">
  <title>spring-security baby-steps</title>
</head>
<body>
<h1>Hello!</h1>
</body>
</html>
```

finally, to gracefully shutdown application under test on CI builds,
add actuator dependency:

```xml
    <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
    </dependency>
```

with according configurations in `application.yaml` file:

```yaml
spring:
  output:
    ansi:
      enabled: always
---
spring:
  profiles: ci
management:
  endpoint:
    shutdown:
      enabled: true
  endpoints:
    web:
      exposure:
        include: >
          shutdown
```

so, you can start application which is supports shutdown, like so:

```bash
java -jar /path/to/jar --spring.profiles.active=ci
```

### test application

use required dependencies:

```xml
  <dependencies>
    <dependency>
      <groupId>com.codeborne</groupId>
      <artifactId>selenide</artifactId>
      <scope>test</scope>
    </dependency>
  </dependencies>
```

implement Selenide test:

```java
@Log4j2
@AllArgsConstructor
class AppTest extends AbstractTest {

  @Test
  void test() {
    open("http://127.0.0.1:8080");
    var h1 = $("h1");
    log.info("h1 html: {}", h1);
    h1.shouldBe(exist, visible)
      .shouldHave(text("hello"));
  }
}
```

see sources for implementation details.

build, run test and cleanup:

```bash
./mvnw -f step-0-application-without-security
java -jar ./step-0-application-without-security/target/*jar --spring.profiles.active=ci &
./mvnw -Dgroups=e2e -f step-0-test-application-without-security
http post :8080/actuator/shutdown
```

## maven

we will be releasing after each important step! so it will be easy simply checkout needed version from git tag.

increment version:

```bash
1.1.1?->1.1.2
./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set -DgenerateBackupPoms=false -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}
```

current release version:

```bash
# 1.2.3-SNAPSHOT -> 1.2.3
./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set -DgenerateBackupPoms=false -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.incrementalVersion}
```

next snapshot version:

```bash
# 1.2.3? -> 1.2.4-SNAPSHOT
./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set -DgenerateBackupPoms=false -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}-SNAPSHOT
```

release version without maven-release-plugin (when you aren't using *-SNAPSHOT version for development):

```bash
currentVersion=`./mvnw -q --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'`
git tag "v$currentVersion"

./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set -DgenerateBackupPoms=false \
  -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}
nextVersion=`./mvnw -q --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'`
__
git add . ; git commit -am "v$currentVersion release." ; git push --tags
```

release version using maven-release-plugin (when you are using *-SNAPSHOT version for development):

```bash
currentVersion=`./mvnw -q --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'`
./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set \
    -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}
developmentVersion=`./mvnw -q --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'`
./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set -DnewVersion="$currentVersion"
./mvnw clean release:prepare release:perform \
    -B -DgenerateReleasePoms=false -DgenerateBackupPoms=false \ 
    -DreleaseVersion="$currentVersion" -DdevelopmentVersion="$developmentVersion"
```

## resources

* [YouTube: Spring Security Basics](https://www.youtube.com/playlist?list=PLqq-6Pq4lTTYTEooakHchTGglSvkZAjnE)
* https://github.com/daggerok/spring-security-examples
* [Official Apache Maven documentation](https://maven.apache.org/guides/index.html)
* [Spring Boot Maven Plugin Reference Guide](https://docs.spring.io/spring-boot/docs/2.3.0.M4/maven-plugin/reference/html/)
* [Create an OCI image](https://docs.spring.io/spring-boot/docs/2.3.0.M4/maven-plugin/reference/html/#build-image)
* [Spring Security](https://docs.spring.io/spring-boot/docs/2.2.6.RELEASE/reference/htmlsingle/#boot-features-security)
* [Spring Configuration Processor](https://docs.spring.io/spring-boot/docs/2.2.6.RELEASE/reference/htmlsingle/#configuration-metadata-annotation-processor)
* [Spring Web](https://docs.spring.io/spring-boot/docs/2.2.6.RELEASE/reference/htmlsingle/#boot-features-developing-web-applications)
* [Securing a Web Application](https://spring.io/guides/gs/securing-web/)
* [Spring Boot and OAuth2](https://spring.io/guides/tutorials/spring-boot-oauth2/)
* [Authenticating a User with LDAP](https://spring.io/guides/gs/authenticating-ldap/)
* [Building a RESTful Web Service](https://spring.io/guides/gs/rest-service/)
* [Serving Web Content with Spring MVC](https://spring.io/guides/gs/serving-web-content/)
* [Building REST services with Spring](https://spring.io/guides/tutorials/bookmarks/)
