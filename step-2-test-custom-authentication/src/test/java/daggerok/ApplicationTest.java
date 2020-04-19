package daggerok;

import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.WebDriverRunner;
import lombok.Value;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.open;
import static org.assertj.core.api.Assertions.assertThat;

@SpringBootApplication
@EnableConfigurationProperties(TestApplicationProps.class)
class TestApplication { }

@Value
@ConstructorBinding
@ConfigurationProperties("test-application-props")
class TestApplicationProps {
  String baseUrl;
  String username, password;
}

@Log4j2
@Tag("e2e")
@SpringBootTest(properties = {
    "test-application-props.base-url=http://127.0.0.1:8080",
    "test-application-props.username=admin",
    "test-application-props.password=admin",
    "spring.output.ansi.enabled=always",
})
class ApplicationTest {

  @Autowired
  ApplicationContext context;

  @Test
  void test() {
    var props = context.getBean(TestApplicationProps.class);
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
    var props = context.getBean(TestApplicationProps.class);
    open(props.getBaseUrl());
    assertThat(WebDriverRunner.driver().url()).endsWith("/login");
  }

  @AfterEach
  void after() {
    Selenide.closeWebDriver();
  }
}
