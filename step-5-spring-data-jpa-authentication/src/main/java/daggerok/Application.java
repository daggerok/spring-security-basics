package daggerok;

import lombok.*;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.util.Optional;

import static lombok.AccessLevel.PROTECTED;

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

@SpringBootApplication
public class Application {
  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }
}
