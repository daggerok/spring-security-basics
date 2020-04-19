package daggerok;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import lombok.AllArgsConstructor;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;

@Value
@ConfigurationProperties("props")
class Props {

  String baseUrl, username, password;

  @ConstructorBinding
  Props(@DefaultValue("http://127.0.0.1:8080") String baseUrl,
        @DefaultValue("user") String username,
        @DefaultValue("pwd") String password) {

    this.baseUrl = baseUrl;
    this.username = username;
    this.password = password;
  }
}

@SpringBootApplication
@EnableConfigurationProperties(Props.class)
class TestContext { }

@Tag("e2e")
@SpringBootTest
@TestInstance(TestInstance.Lifecycle.PER_METHOD)
abstract class AbstractTest {

  @AfterEach
  void after() {
    Selenide.closeWebDriver();
  }
}

@Log4j2
@AllArgsConstructor
class AppTest extends AbstractTest {

  ApplicationContext context;

  @Test
  void test() {
    var props = context.getBean(Props.class);
    open(props.getBaseUrl());

    $("#username").setValue(props.getUsername());
    $("#password").setValue(props.getPassword()).submit();

    var h1 = $("h1");
    log.info("h1 html: {}", h1);
    h1.shouldBe(exist, visible)
      .shouldHave(text("hello"));
  }

  @Test
  void test_login_redirect() {
    var props = context.getBean(Props.class);
    open(props.getBaseUrl());
    assertThat(WebDriverRunner.driver().url()).endsWith("/login");
  }
}
