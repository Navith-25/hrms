package com.nibm.hr.hrms.config;

import com.nibm.hr.hrms.security.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider(CustomUserDetailsService userDetailsService) {
        DaoAuthenticationProvider auth = new DaoAuthenticationProvider();
        auth.setUserDetailsService(userDetailsService);
        auth.setPasswordEncoder(passwordEncoder());
        return auth;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http.authorizeHttpRequests(authorize -> authorize
                        // 1. Static Resources & Login
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/login", "/register").permitAll()

                        // 2. API Endpoints
                        .requestMatchers("/api/**").authenticated()

                        // 3. Dashboard Access
                        .requestMatchers("/").hasAnyRole("ADMIN", "HR_MANAGER", "HR_STAFF", "FINANCE", "DIRECTOR", "MANAGER", "EMPLOYEE")

                        // 4. Messaging
                        .requestMatchers("/messages/**").authenticated()

                        // 5. System Reset
                        .requestMatchers("/admin/system/**").hasRole("ADMIN")

                        // 6. Loan Management
                        .requestMatchers("/loan/request", "/loan/save").authenticated()
                        .requestMatchers("/finance/loans/**", "/finance/loan/**").hasAnyRole("FINANCE", "HR_MANAGER", "DIRECTOR", "MANAGER")

                        // 7. Attendance & Calendar
                        .requestMatchers("/admin/attendance/**").hasRole("HR_STAFF")
                        .requestMatchers("/my-calendar").authenticated()

                        // 8. Training Module
                        .requestMatchers("/admin/training/**").hasRole("DIRECTOR")
                        // FIXED: Added FINANCE role here
                        .requestMatchers("/manager/training/**").hasAnyRole("MANAGER", "HR_MANAGER", "FINANCE")

                        // My Trainings (Employee Only)
                        .requestMatchers("/employee/trainings").hasRole("EMPLOYEE")

                        // 9. Specific GET Rules
                        .requestMatchers(HttpMethod.GET, "/showNewEmployeeForm", "/showFormForUpdate/**")
                        .hasAnyRole("HR_STAFF", "ADMIN", "HR_MANAGER", "DIRECTOR")
                        .requestMatchers(HttpMethod.GET, "/admin/leave")
                        .hasAnyRole("HR_STAFF", "ADMIN", "HR_MANAGER", "DIRECTOR")
                        .requestMatchers(HttpMethod.GET, "/admin/performance/list/**")
                        .hasAnyRole("HR_STAFF", "ADMIN", "HR_MANAGER", "DIRECTOR")

                        // 10. Admin Actions
                        .requestMatchers(HttpMethod.POST, "/admin/employee/saveNew", "/admin/employee/update")
                        .hasAnyRole("ADMIN", "HR_MANAGER", "HR_STAFF", "DIRECTOR")
                        .requestMatchers(HttpMethod.POST, "/deleteEmployee/**")
                        .hasAnyRole("ADMIN", "HR_MANAGER", "DIRECTOR")

                        // 11. Admin Sub-Modules
                        .requestMatchers("/admin/performance/**").hasAnyRole("ADMIN", "HR_MANAGER", "HR_STAFF", "DIRECTOR")
                        .requestMatchers("/admin/payroll/**").hasAnyRole("ADMIN", "HR_MANAGER", "FINANCE", "DIRECTOR")
                        .requestMatchers("/admin/leave/**").hasAnyRole("ADMIN", "HR_MANAGER", "HR_STAFF", "DIRECTOR")
                        .requestMatchers("/admin/reports/**", "/admin/report/**").hasAnyRole("ADMIN", "DIRECTOR")

                        // 12. Manager & Employee Specifics
                        // FIXED: Added FINANCE role to tasks and general manager areas
                        .requestMatchers("/manager/tasks/**").hasAnyRole("MANAGER", "HR_MANAGER", "FINANCE")
                        .requestMatchers("/manager/team", "/manager/leave", "/manager/performance/**").hasAnyRole("MANAGER", "HR_MANAGER", "FINANCE")
                        .requestMatchers(HttpMethod.POST, "/manager/leave/approve/**", "/manager/leave/reject/**").hasAnyRole("MANAGER", "HR_MANAGER", "FINANCE")

                        // Personal Essentials
                        .requestMatchers("/leave", "/leave/request", "/payslips", "/performance").authenticated()

                        // My Tasks (Employee Only)
                        .requestMatchers("/employee/tasks/**").hasRole("EMPLOYEE")

                        .requestMatchers(HttpMethod.GET, "/payslip/{id}").hasAnyRole("EMPLOYEE", "ADMIN", "HR_MANAGER", "DIRECTOR", "FINANCE", "MANAGER", "HR_STAFF")

                        // 13. General Catch-All
                        .requestMatchers("/admin/**").hasAnyRole("ADMIN", "DIRECTOR")
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .loginProcessingUrl("/login")
                        .defaultSuccessUrl("/", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                .csrf(csrf -> csrf.disable());

        return http.build();
    }
}