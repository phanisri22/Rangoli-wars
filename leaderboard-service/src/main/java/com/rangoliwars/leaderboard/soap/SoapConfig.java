package com.rangoliwars.leaderboard.soap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.ws.wsdl.wsdl11.DefaultWsdl11Definition;
import org.springframework.xml.xsd.SimpleXsdSchema;
import org.springframework.xml.xsd.XsdSchema;

/** WSDL is generated from the XSD and served at /soap/leaderboard.wsdl. */
@Configuration
public class SoapConfig {

    public static final String NAMESPACE = "http://rangoliwars.com/leaderboard";

    @Bean(name = "leaderboard")
    DefaultWsdl11Definition leaderboardWsdl(XsdSchema leaderboardSchema) {
        DefaultWsdl11Definition definition = new DefaultWsdl11Definition();
        definition.setPortTypeName("LeaderboardPort");
        definition.setLocationUri("/soap");
        definition.setTargetNamespace(NAMESPACE);
        definition.setSchema(leaderboardSchema);
        return definition;
    }

    @Bean
    XsdSchema leaderboardSchema() {
        return new SimpleXsdSchema(new ClassPathResource("soap/leaderboard.xsd"));
    }
}
