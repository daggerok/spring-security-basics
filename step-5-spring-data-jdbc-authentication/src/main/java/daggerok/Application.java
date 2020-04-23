package daggerok;

import lombok.RequiredArgsConstructor;
import lombok.Value;
import lombok.With;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.data.annotation.Id;
import org.springframework.data.jdbc.repository.query.Query;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;
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

import java.util.Optional;

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
