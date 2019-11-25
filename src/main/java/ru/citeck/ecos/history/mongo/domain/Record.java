package ru.citeck.ecos.history.mongo.domain;

import com.mongodb.BasicDBObject;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.HashMap;
import java.util.Map;

@Data
public class Record {

    @Id
    private String id;

    @Field("ext_id")
    private String extId;

    @Field("attr")
    private BasicDBObject attributes;

    public Map getAttributes() {
        if (attributes == null) {
            return new HashMap();
        }
        return attributes.toMap();
    }

    public void setAttributes(Map attributes) {
        this.attributes = new BasicDBObject(attributes);
    }

}
