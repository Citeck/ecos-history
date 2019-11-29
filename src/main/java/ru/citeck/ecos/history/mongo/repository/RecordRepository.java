package ru.citeck.ecos.history.mongo.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import ru.citeck.ecos.history.mongo.domain.Record;

public interface RecordRepository extends MongoRepository<Record, String> {

}
