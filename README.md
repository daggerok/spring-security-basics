# spring-security-basics [![CI](https://github.com/daggerok/spring-security-basics/workflows/CI/badge.svg)](https://github.com/daggerok/spring-security-basics/actions?query=workflow%3ACI)
Learn Spring Security by baby steps from zero to pro! (Status: IN PROGRESS)

## Table of Content

* [Step 0: No security](#step-0)
* [Step 1: Add authentication](#step-1)
* [Step 2: Custom authentication](#step-2)
* [Step 3: Add authorization](#step-3)
* [Step 4: JavaEE and Spring Security](#step-4)
* [Step 5.1: JDBC authentication](#step-51)
* [Step 5.2: Spring Data JDBC authentication](#step-52)
* [Step 5.3: Spring Data JPA authentication](#step-53)
* [Versioning and releasing](#maven)
* [Resources and used links](#resources)

## step: 0

let's use simple spring boot web app without security at all!

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

add in `Application.java` file controller for index page:

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
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
</dependencies>
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
class ApplicationTest extends AbstractTest {

  @Test
  void test() {
    open("http://127.0.0.1:8080"); // open home page...
    var h1 = $("h1");              // find there <h1> tag...
    log.info("h1 html: {}", h1);
    h1.shouldBe(exist, visible)    // element should be inside DOM
      .shouldHave(text("hello"));  // textContent of the tag should
                                   // contains expected content...
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

## step: 1

in this step we are going to implement simple authentication.
it's mean everyone who logged in, can access all available
resources.

### application

add required dependencies:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-security</artifactId>
  </dependency>
</dependencies>
```

update `application.yaml` configuration with desired user password:

```yaml
spring:
  security:
    user:
      password: pwd
```

tune little bit security config to bein able shutdown application with POST:
we have to permit it and disable CSRF:

```java
@EnableWebSecurity
class MyWebSecurity extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
          .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
          .anyRequest().authenticated()
        .and()
          .csrf().disable()
        .formLogin()
    ;
  }
}
```

### test application

now, let's update test according to configured security as follows:

```java
@Log4j2
@AllArgsConstructor
class ApplicationTest extends AbstractTest {

  @Test
  void test() {
    open("http://127.0.0.1:8080");
    // we should be redirected to login page, so lets authenticate!
    $("#username").setValue("user");
    $("#password").setValue("pwd").submit();
    // everything else is with no changes...
    var h1 = $("h1");
    log.info("h1 html: {}", h1);
    h1.shouldBe(exist, visible)
      .shouldHave(text("hello"));
  }
}
```

build, run test and cleanup:

```bash
./mvnw -f step-0-application-without-security
SPRING_PROFILES_ACTIVE=ci java -jar ./step-0-application-without-security/target/*jar &
./mvnw -Dgroups=e2e -f step-0-test-application-without-security
http post :8080/actuator/shutdown
```

## step: 2

let's add few users for authorization:

```java
@EnableWebSecurity
class MyWebSecurity extends WebSecurityConfigurerAdapter {

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.inMemoryAuthentication()
          .withUser("user")
            .password(passwordEncoder().encode("password"))
            .roles("USER")
            .and()
          .withUser("admin")
            .password(passwordEncoder().encode("admin"))
            .roles("USER", "ADMIN")
        ;
  }

  // ...
}
```

now we can authenticate with `users`/`password` or `admin`/`admin`

## step: 3

now let's add authorization, so we can distinguish that different users
have access to some resources where others are not!

### application

in next configuration access to `/admin` path:

```java
@Controller
class AdminPage {

  @GetMapping("admin")
  String index() {
    return "admin/index.html";
  }
}
```

add `admin/index.html` file:

```html
<!doctype html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <meta http-equiv="X-UA-Compatible" content="ie=edge">
  <title>Admin Page | spring-security baby-steps</title>
</head>
<body>
<h2>Administration page</h2>
</body>
</html>
```

we can allow to users with admin role:

```java
@EnableWebSecurity
class MyWebSecurity extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
          .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
          .antMatchers("/admin/**").hasRole("ADMIN")
          .anyRequest().authenticated()
        .and()
          .csrf().disable()
        .formLogin()
    ;
  }
}
``` 

### test application

```java
@Value
@ConstructorBinding
@ConfigurationProperties("test-application-props")
class TestApplicationProps {

  String baseUrl;
  User admin;
  User user;

  @Value
  @ConstructorBinding
  static class User {
    String username;
    String password;
  }
}

@Log4j2
@Tag("e2e")
@AllArgsConstructor
@SpringBootTest(properties = {
    "test-application-props.user.username=user",
    "test-application-props.user.password=password",
    "test-application-props.admin.username=admin",
    "test-application-props.admin.password=admin",
    "test-application-props.base-url=http://127.0.0.1:8080",
})
class ApplicationTest {

  ApplicationContext context;

  @Test
  void admin_should_authorize() {
    var props = context.getBean(TestApplicationProps.class);
    open(String.format("%s/admin", props.getBaseUrl()));
    $("#username").setValue(props.getAdmin().getUsername());
    $("#password").setValue(props.getAdmin().getPassword()).submit();

    var h2 = $("h2");
    log.info("h2 html: {}", h2);
    h2.shouldBe(exist, visible)
      .shouldHave(text("administration"));
  }

  @Test
  void test_forbidden_403() {
    var props = context.getBean(TestApplicationProps.class);
    open(String.format("%s/admin", props.getBaseUrl()));
    $("#username").setValue(props.getUser().getUsername());
    $("#password").setValue(props.getUser().getPassword()).submit();
    $(withText("403")).shouldBe(exist, visible);
    $(withText("Forbidden")).shouldBe(exist, visible);
  }

  @AfterEach
  void after() {
    closeWebDriver();
  }
}
```

## step: 4

let's try use Spring Security together with JavaEE!
NOTE: use spring version 4.x, not 5!

in this step we will configure JavaEE app for next
sets of security rules:

allowed for all: `/`, `/favicon.ico`, `/api/health`, `/login`, `/logout`
allowed for admins only: `/admin`
all other paths allowed only for authenticated users.

### application

dependencies:

```xml
<dependencies>
  <dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-config</artifactId>
  </dependency>
  <dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-taglibs</artifactId>
  </dependency>
</dependencies>
```

JAX-RS application:

```java
@ApplicationScoped
@ApplicationPath("api")
public class Config extends Application { }

@Path("")
@RequestScoped
@Produces(APPLICATION_JSON)
public class HealthResource {

  @GET
  @Path("health")
  public JsonObject hello() {
    return Json.createObjectBuilder()
               .add("status", "UP")
               .build();
  }
}

@Path("v1")
@RequestScoped
@Produces(APPLICATION_JSON)
public class MyResource {

  @GET
  @Path("hello")
  public JsonObject hello() {
    return Json.createObjectBuilder()
               .add("hello", "world!")
               .build();
  }
}
```

Spring Security configuration:

```java
@Configuration
@EnableWebSecurity
public class SpringSecurityConfig extends WebSecurityConfigurerAdapter {

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    // @formatter:off
    auth.inMemoryAuthentication()
          .withUser("user")
          .password("password")
          .roles("USER")
        .and()
          .withUser("admin")
          .password("admin")
          .roles("ADMIN")
    // @formatter:on
    ;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    // @formatter:off
    http.authorizeRequests()
          .antMatchers("/", "/favicon.ico", "/api/health").permitAll()
          .antMatchers("/admin/**").hasRole("ADMIN")
          .anyRequest().authenticated()
        .and()
          .formLogin()
        .and()
          .logout()
            .logoutSuccessUrl("/")
            .clearAuthentication(true)
            .invalidateHttpSession(true)
            .deleteCookies("JSESSIONID")
        .and()
          .csrf().disable()
        .sessionManagement()
          .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
    // @formatter:on
    ;
  }
}

public class SecurityWebApplicationInitializer extends AbstractSecurityWebApplicationInitializer {

  public SecurityWebApplicationInitializer() {
    super(SpringSecurityConfig.class);
  }
}
```

add `src/main/resources/META-INF/beans.xml` file:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<beans xmlns="http://xmlns.jcp.org/xml/ns/javaee"
       xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
       xsi:schemaLocation="http://xmlns.jcp.org/xml/ns/javaee http://xmlns.jcp.org/xml/ns/javaee/beans_1_1.xsd"
       bean-discovery-mode="annotated">
</beans>
```

finally, add HTML pages:

file `src/main/webapp/index.html`:

```html
<!doctype html>
<html lang="en">
<head>
  <title>Hello!</title>
</head>
<body>
  <h1>Hello!</h1>
</body>
</html>
```

file `src/main/webapp/admin/index.html`:

```html
<!doctype html>
<html lang="en">
<head>
  <title>Admin</title>
</head>
<body>
  <h1>Admin page</h1>
</body>
</html>
```

### test application

```bash
./mvnw -f step-4-java-ee-jaxrs-jboss-spring-security
./mvnw -f step-4-java-ee-jaxrs-jboss-spring-security docker:build docker:start
./mvnw -f step-4-test-java-ee-jboss-spring-security -Dgroups=e2e
./mvnw -f step-4-java-ee-jaxrs-jboss-spring-security docker:stop docker:remove
```

## step: 5.1

let's use jdbc database as users / roles store.

security config:

```java
@EnableWebSecurity
@RequiredArgsConstructor
class MyWebSecurity extends WebSecurityConfigurerAdapter {

  final DataSource dataSource;

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.jdbcAuthentication()
          .dataSource(dataSource)
          .usersByUsernameQuery(
             " select sec_username, sec_password, sec_enabled " +
             " from sec_users where sec_username=?            "
          )
          .authoritiesByUsernameQuery(
            " select sec_username, sec_authority        " +
            " from sec_authorities where sec_username=? "
          );
    ;
  }

  // ...
}
```

sql schema and data:

```sql
drop index if exists sec_authorities_idx;
drop table if exists sec_authorities;
drop table if exists sec_users;
drop schema if exists "public";

create schema "public";

create table sec_users (
  sec_username varchar(255) not null primary key,
  sec_password varchar(1024) not null,
  sec_enabled boolean not null
);

create table sec_authorities (
  sec_username varchar(255) not null,
  sec_authority varchar(255) not null,
  constraint sec_authorities_fk
    foreign key (sec_username)
      references sec_users (sec_username)
);

create unique index sec_authorities_idx
  on sec_authorities (sec_username, sec_authority);

insert into sec_users (sec_username, sec_password, sec_enabled)
values ('user', '{bcrypt}$2a$10$OlBp2JOK0/8xDjiVqh4OYOggr3tHTKfBcv82dso4fsnUPo66f5Ury', true),  -- password
       ('admin', '{bcrypt}$2a$10$OKPak8tw3jYSyqil/eNKz.U1nF/HtabOotUqi2ceeLuWdBsejH9yS', true); -- admin
insert into sec_authorities (sec_username, sec_authority)
values ('user', 'ROLE_USER'),
       ('admin', 'ROLE_ADMIN');
```

testing:

```bash
./mvnw -f step-5-jdbc-authentication clean package spring-boot:build-image docker-compose:up
while ! [[ `curl -s -o /dev/null -w "%{http_code}" 0:8080/actuator/health` -eq 200 ]] ; do sleep 1s ; echo -n '.' ; done
./mvnw -f step-5-test-jdbc -Dgroups=e2e 
./mvnw -f step-5-jdbc-authentication docker-compose:down
```

## step: 5.2

let's use spring-data-jdbc database as users / roles store.

add security entity, repository and service:

```java
@With
@Value
@Table("sec_users")
class Security {

  @Id
  @Column("sec_username") String username;
  @Column("sec_password") String password;
  @Column("sec_enabled") boolean active;
  @Column("sec_authority") String authority;

  public UserDetails toUserDetails() {
    return User.builder()
               .username(username)
               .password(password)
               .disabled(!active)
               .accountExpired(!active)
               .credentialsExpired(!active)
               .authorities(AuthorityUtils.createAuthorityList(authority))
               .build();
  }
}

interface SecurityRepository extends CrudRepository<Security, String> {

  @Query("select * from sec_users where sec_username = :username limit 1")
  Optional<Security> findFirstByUsername(@Param("username") String username);
}

@Service
@RequiredArgsConstructor
class SecurityService implements UserDetailsService {

  final SecurityRepository securityRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return securityRepository.findFirstByUsername(username)
                             .map(Security::toUserDetails)
                             .orElseThrow(() -> new UsernameNotFoundException(
                                 String.format("User %s not found.", username)));
  }
}
```

security config:

```java
@EnableWebSecurity
@RequiredArgsConstructor
class MyWebSecurity extends WebSecurityConfigurerAdapter {

  final SecurityService securityService;

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(securityService);
  }

  // ...
}
```

sql schema and data:

```sql
drop index if exists sec_users_authorities_idx;
drop table if exists sec_users;
drop schema if exists "public";

create schema "public";

create table sec_users (
  sec_username varchar(255) not null primary key,
  sec_password varchar(1024) not null,
  sec_enabled boolean not null,
  sec_authority varchar(255) not null
);

create unique index sec_users_authorities_idx
  on sec_users (sec_username, sec_authority);

insert into sec_users (sec_username, sec_password, sec_enabled, sec_authority)
values ('user', '{bcrypt}$2a$10$OlBp2JOK0/8xDjiVqh4OYOggr3tHTKfBcv82dso4fsnUPo66f5Ury', true, 'ROLE_USER')
,      ('admin', '{bcrypt}$2a$10$OKPak8tw3jYSyqil/eNKz.U1nF/HtabOotUqi2ceeLuWdBsejH9yS', true, 'ROLE_ADMIN')
;
```

testing:

```bash
./mvnw -f step-5-spring-data-jdbc-authentication clean package spring-boot:build-image docker-compose:up
./mvnw -f step-5-test-jdbc -Dgroups=e2e 
./mvnw -f step-5-spring-data-jdbc-authentication docker-compose:down
```

## step: 5.3

let's use spring-data-jpa this time.

required changes:

```java
@Data
@Entity
@Setter(PROTECTED)
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(staticName = "of")
@Table(name = "sec_users")
class Security {

  @Id
  @Column(nullable = false, name = "sec_username")
  private String username;

  @Column(nullable = false, name = "sec_password")
  private String password;

  @Column(nullable = false, name = "sec_enabled")
  private boolean active;

  @Column(nullable = false, name = "sec_authority")
  private String authority;

  public UserDetails toUserDetails() {
    return User.builder()
               .username(username)
               .password(password)
               .disabled(!active)
               .accountExpired(!active)
               .credentialsExpired(!active)
               .authorities(AuthorityUtils.createAuthorityList(authority))
               .build();
  }
}

interface SecurityRepository extends CrudRepository<Security, String> {

  @Query
  Optional<Security> findFirstByUsername(@Param("username") String username);
}

@Service
@RequiredArgsConstructor
class SecurityService implements UserDetailsService {

  final SecurityRepository securityRepository;

  @Override
  public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
    return securityRepository.findFirstByUsername(username)
                             .map(Security::toUserDetails)
                             .orElseThrow(() -> new UsernameNotFoundException(
                                 String.format("User %s not found.", username)));
  }
}

@EnableWebSecurity
@RequiredArgsConstructor
class MyWebSecurity extends WebSecurityConfigurerAdapter {

  final SecurityService securityService;

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.userDetailsService(securityService);
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
          .requestMatchers(EndpointRequest.to("health", "shutdown")).permitAll()
          .antMatchers("/", "/favicon.ico", "/assets/**").permitAll()
          .antMatchers("/admin/**").hasRole("ADMIN")
          .anyRequest().authenticated()
        .and()
          .csrf().disable()
        .formLogin()
          .and()
        .httpBasic()
    ;
  }
}
```

_application.yaml` file:

```yaml
spring:
  datasource:
    driver-class-name: org.postgresql.Driver
    url: jdbc:postgresql://${POSTGRES_HOST:127.0.0.1}:${POSTGRES_PORT:5432}/${POSTGRES_DB:postgres}
    username: ${POSTGRES_USER:postgres}
    password: ${POSTGRES_PASSWORD:postgres}
  flyway:
    enabled: true
  jpa:
    database: postgresql
    generate-ddl: false
    show-sql: true
    hibernate:
      ddl-auto: validate
    properties:
      hibernate:
        temp:
          use_jdbc_metadata_defaults: false
```

`db/migration` scripts:

```sql
create table sec_users (
  sec_username varchar(255) not null primary key,
  sec_password varchar(1024) not null,
  sec_enabled boolean not null,
  sec_authority varchar(255) not null
)
;
create unique index sec_users_authorities_idx
  on sec_users (sec_username, sec_authority)
;
insert into sec_users (sec_username, sec_password, sec_enabled, sec_authority)
values ('user', '{bcrypt}$2a$10$OlBp2JOK0/8xDjiVqh4OYOggr3tHTKfBcv82dso4fsnUPo66f5Ury', true, 'ROLE_USER')
,      ('admin', '{bcrypt}$2a$10$OKPak8tw3jYSyqil/eNKz.U1nF/HtabOotUqi2ceeLuWdBsejH9yS', true, 'ROLE_ADMIN')
;
```

testing:

```bash
# docker-compose -f step-5-spring-data-jpa-authentication/docker-compose.yaml up postgres
./mvnw -f step-5-spring-data-jpa-authentication clean package spring-boot:build-image docker-compose:up
./mvnw -f step-5-test-jdbc -Dgroups=e2e 
./mvnw -f step-5-spring-data-jpa-authentication docker-compose:down
```

## maven

we will be releasing after each important step! so it will be easy simply checkout needed version from git tag.

release version without maven-release-plugin (when you aren't using *-SNAPSHOT version for development):

```bash
currentVersion=`./mvnw -q --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'`
git tag "v$currentVersion"

./mvnw build-helper:parse-version -DgenerateBackupPoms=false -DgenerateBackupPoms=false versions:set \
  -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion} \
  -f step-4-java-ee-jaxrs-jboss-spring-security
./mvnw build-helper:parse-version -DgenerateBackupPoms=false versions:set -DgenerateBackupPoms=false \
  -DnewVersion=\${parsedVersion.majorVersion}.\${parsedVersion.minorVersion}.\${parsedVersion.nextIncrementalVersion}
nextVersion=`./mvnw -q --non-recursive exec:exec -Dexec.executable=echo -Dexec.args='${project.version}'`

git add . ; git commit -am "v$currentVersion release." ; git push --tags
```

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

## resources

* (Modern Spring Security for Spring Actuator endpoints)[https://youtu.be/SSu7V-S5yec?t=520]
* [YouTube: Spring Security Basics](https://www.youtube.com/playlist?list=PLqq-6Pq4lTTYTEooakHchTGglSvkZAjnE)
* https://github.com/daggerok/spring-security-examples
<!--
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
-->
