package ru.citeck.ecos.history.records.tasks;

import com.google.common.collect.Lists;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import ru.citeck.ecos.history.aop.UsernameModelProviderAdvice;
import ru.citeck.ecos.records2.RecordRef;
import ru.citeck.ecos.records2.RecordsService;

import static org.junit.Assert.assertEquals;

public class CacheableCurrentActorsProviderTest {

    private CacheableCurrentActorsProvider provider;

    private final RecordRef adminRef = RecordRef.create("alfresco", "people", "admin");
    private final RecordRef someUserRef = RecordRef.create("alfresco", "people", "some");

    @Before
    public void setUp() {
        RecordsService recordsService = Mockito.mock(RecordsService.class);

        CacheableCurrentActorsProvider.UserDTO adminDto = new CacheableCurrentActorsProvider.UserDTO();
        adminDto.setUserName("admin");
        adminDto.setAuthorities(Lists.newArrayList("admin", "GROUP_SOME"));

        Mockito.when(recordsService.getMeta(adminRef, CacheableCurrentActorsProvider.UserDTO.class))
            .thenReturn(adminDto)
            .thenThrow(new IllegalArgumentException("Must be requested only one time"));

        CacheableCurrentActorsProvider.UserDTO someUserDto = new CacheableCurrentActorsProvider.UserDTO();
        someUserDto.setUserName("some");
        someUserDto.setAuthorities(Lists.newArrayList("some"));

        Mockito.when(recordsService.getMeta(someUserRef, CacheableCurrentActorsProvider.UserDTO.class))
            .thenReturn(someUserDto)
            .thenThrow(new IllegalArgumentException("Must be requested only one time"));

        provider = new CacheableCurrentActorsProvider(recordsService);
    }

    @Test
    public void getCurrentUserAuthorities() {
        setCurrentRequestUsername("admin");
        assertEquals(2, provider.getCurrentUserAuthorities().size());
        assertEquals(2, provider.getCurrentUserAuthorities().size());

        setCurrentRequestUsername("some");
        assertEquals(1, provider.getCurrentUserAuthorities().size());
        assertEquals(1, provider.getCurrentUserAuthorities().size());
    }

    private void setCurrentRequestUsername(String username) {
        ServletRequestAttributes attributes = new ServletRequestAttributes(new MockHttpServletRequest());
        attributes.setAttribute(
            UsernameModelProviderAdvice.REQUEST_USERNAME,
            username,
            RequestAttributes.SCOPE_REQUEST);
        RequestContextHolder.setRequestAttributes(attributes);
    }

}
