package ru.citeck.ecos.history.mongo.repository;

import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.repository.MongoRepository;
import ru.citeck.ecos.history.mongo.domain.Record;

@Profile("facade")
public interface RecordRepository extends MongoRepository<Record, String> {

}
