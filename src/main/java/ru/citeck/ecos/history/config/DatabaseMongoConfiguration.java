package ru.citeck.ecos.history.config;

import com.github.cloudyrock.mongock.SpringMongock;
import com.github.cloudyrock.mongock.SpringMongockBuilder;
import com.mongodb.MongoClient;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Profile;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.core.mapping.event.ValidatingMongoEventListener;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import ru.citeck.ecos.webapp.lib.spring.context.utils.JSR310DateConverters;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Roman Makarskiy
 */
@Profile("facade")
@Configuration
@EnableMongoRepositories("ru.citeck.ecos.history.mongo.repository")
@Import(value = MongoAutoConfiguration.class)
@EnableMongoAuditing(auditorAwareRef = "springSecurityAuditorAware")
public class DatabaseMongoConfiguration {

    private static final String MONGO_CHANGELOG_PACKAGE = "ru.citeck.ecos.history.mongo.dbmigrations";

    @Bean
    public ValidatingMongoEventListener validatingMongoEventListener() {
        return new ValidatingMongoEventListener(validator());
    }

    @Bean
    public LocalValidatorFactoryBean validator() {
        return new LocalValidatorFactoryBean();
    }

    @Bean
    public MongoCustomConversions customConversions() {
        List<Converter<?, ?>> converters = new ArrayList<>();
        converters.add(JSR310DateConverters.DateToZonedDateTimeConverter.INSTANCE);
        converters.add(JSR310DateConverters.ZonedDateTimeToDateConverter.INSTANCE);
        return new MongoCustomConversions(converters);
    }

    @Bean
    public SpringMongock mongock(MongoClient mongoClient, MongoProperties mongoProperties) {
        String mongoDbName = mongoProperties.getMongoClientDatabase();
        return new SpringMongockBuilder(mongoClient, mongoDbName, MONGO_CHANGELOG_PACKAGE)
            .setLockQuickConfig()
            .build();
    }

}
