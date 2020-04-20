package daggerok;

import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;

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
          .antMatchers("/", "/index.html", "/favicon.ico", "/api/health").permitAll()
          .antMatchers("/admin/**").hasRole("ADMIN")
          .anyRequest().authenticated()
        //.and().httpBasic()
        .and()
          .formLogin()//.defaultSuccessUrl("/").failureUrl("/login?error=true")//.defaultSuccessUrl("/home", true)
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
