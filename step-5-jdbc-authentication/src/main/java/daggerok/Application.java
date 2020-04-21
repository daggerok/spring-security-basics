package daggerok;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jdbc.datasource.DelegatingDataSource;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import javax.sql.DataSource;

@Controller
class IndexPage {

  @GetMapping("/")
  String index() {
      return "index.html";
  }
}

@Controller
class UserPage {

  @GetMapping("user")
  String index() {
    return "user/index.html";
  }
}

@Controller
class AdminPage {

  @GetMapping("admin")
  String index() {
    return "admin/index.html";
  }
}

@EnableWebSecurity
@RequiredArgsConstructor
class MyWebSecurity extends WebSecurityConfigurerAdapter {

  final DataSource dataSource;

  @Bean
  PasswordEncoder passwordEncoder() {
    return PasswordEncoderFactories.createDelegatingPasswordEncoder();
  }

  @Override
  protected void configure(AuthenticationManagerBuilder auth) throws Exception {
    auth.jdbcAuthentication()
          .dataSource(dataSource)
          // .withDefaultSchema() // doesn't work with postgres by default
          .usersByUsernameQuery("select sec_username, sec_password, sec_enabled from sec_users where sec_username=?")
          .authoritiesByUsernameQuery("select sec_username, sec_authority from sec_authorities where sec_username=?");
    ;
  }

  @Override
  protected void configure(HttpSecurity http) throws Exception {
    http.authorizeRequests()
          .requestMatchers(EndpointRequest.toAnyEndpoint()).permitAll()
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

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
