package com.pingpad.modules.eventsourcing.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.databind.jsontype.PolymorphicTypeValidator;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Configuration for event sourcing infrastructure.
 */
@Configuration
public class EventSourcingConfig {

    @Bean
    @Primary
    public ObjectMapper eventSourcingObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Configure for polymorphic event deserialization
        PolymorphicTypeValidator ptv = BasicPolymorphicTypeValidator.builder()
            .allowIfSubType("com.pingpad.modules")
            .build();
        mapper.activateDefaultTyping(
            ptv,
            com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping.NON_FINAL,
            com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY
        );
        return mapper;
    }
}
