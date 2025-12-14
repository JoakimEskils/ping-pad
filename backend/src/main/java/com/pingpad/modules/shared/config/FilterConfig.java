package com.pingpad.modules.shared.config;

import com.pingpad.modules.shared.filters.CorrelationIdFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;

/**
 * Configuration to register servlet filters with proper ordering.
 * CorrelationIdFilter needs to run first to ensure correlation IDs are available
 * for all subsequent filters and request processing.
 * 
 * By explicitly registering the filter here, we ensure it runs before Spring Security
 * filters and any other servlet filters.
 */
@Configuration
public class FilterConfig {

    @Bean
    public FilterRegistrationBean<CorrelationIdFilter> correlationIdFilterRegistration(CorrelationIdFilter filter) {
        FilterRegistrationBean<CorrelationIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(filter);
        registration.addUrlPatterns("/*");
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE); // Run before all other filters
        registration.setName("correlationIdFilter");
        // Disable default registration to avoid duplicate registration
        registration.setEnabled(true);
        return registration;
    }
}
