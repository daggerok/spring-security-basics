package daggerok;

import com.codeborne.selenide.Selenide;
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

@Value
@ConfigurationProperties("props")
class Props {

  String baseUrl;

  @ConstructorBinding
  Props(@DefaultValue("http://127.0.0.1:8080") String baseUrl) {
    this.baseUrl = baseUrl;
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

    var h1 = $("h1");
    log.info("h1 html: {}", h1);
    h1.shouldBe(exist, visible)
      .shouldHave(text("hello"));
  }
}
