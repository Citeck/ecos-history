package ru.citeck.ecos.history.records.tasks;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ru.citeck.ecos.history.security.SecurityUtils;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;
import ru.citeck.ecos.records2.graphql.meta.annotation.MetaAtt;
import ru.citeck.ecos.records2.spring.RemoteRecordsUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
public class CacheableCurrentActorsProvider {

    private LoadingCache<String, List<String>> actorCache;
    private RecordsService recordsService;

    @Autowired
    public CacheableCurrentActorsProvider(RecordsService recordsService) {
        this.recordsService = recordsService;

        actorCache = CacheBuilder.newBuilder()
            .expireAfterWrite(10, TimeUnit.MINUTES)
            .build(CacheLoader.from(this::findActorsByKey));
    }

    public List<String> getCurrentUserAuthorities() {
        Optional<String> currentUserLogin = SecurityUtils.getCurrentUsername();
        if (!currentUserLogin.isPresent()) {
            return Collections.emptyList();
        }

        String username = currentUserLogin.get();
        return actorCache.getUnchecked(username);
    }

    private List<String> findActorsByKey(String username) {
        RecordRef userRef = createRemoteRef(username);
        UserDTO user = RemoteRecordsUtils.runAsSystem(() -> recordsService.getMeta(userRef, UserDTO.class));

        if (user != null && CollectionUtils.isNotEmpty(user.authorities)) {
            return user.authorities;
        }
        return Collections.emptyList();
    }

    private RecordRef createRemoteRef(String username) {
        return RecordRef.create("alfresco", "people", username);
    }

    @Data
    static class UserDTO {
        private String userName;
        @MetaAtt("authorities.list[]")
        private List<String> authorities;
    }

}
