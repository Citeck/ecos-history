package ru.citeck.ecos.history.records.tasks;

import com.google.common.collect.Lists;
import org.apache.commons.collections4.IterableUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import ru.citeck.ecos.records2.request.query.RecordsQuery;
import ru.citeck.ecos.records2.request.query.SortBy;
import ru.citeck.ecos.records2.request.query.page.SkipPage;

import java.util.List;

import static org.junit.Assert.*;

public class TaskCriteriaBuilderTest {

    private CacheableCurrentActorsProvider provider;
    private TaskCriteriaBuilder criteriaBuilder;

    @Before
    public void setUp() throws Exception {
        provider = Mockito.mock(CacheableCurrentActorsProvider.class);
        Mockito.when(provider.getCurrentUserAuthorities()).thenReturn(Lists.newArrayList("admin"));

        criteriaBuilder = new TaskCriteriaBuilder();
        criteriaBuilder.setCurrentActorsProvider(provider);
    }

    @Test
    public void buildPageable() {
        RecordsQuery query = new RecordsQuery();
        query.setPage(new SkipPage(40, 20));

        Pageable pageable = criteriaBuilder.buildPageable(query);
        assertEquals(2, pageable.getPageNumber());
        assertEquals(20, pageable.getPageSize());
    }

    @Test
    public void buildPageableSortingStandard() {
        RecordsQuery query = new RecordsQuery();
        query.setPage(new SkipPage(40, 20));
        query.setSortBy(Lists.newArrayList(
            new SortBy("cm:created", true),
            new SortBy("cm:modified", false)
        ));

        Pageable pageable = criteriaBuilder.buildPageable(query);
        Sort sort = pageable.getSort();
        List<Sort.Order> orderList = IteratorUtils.toList(sort.iterator());

        assertEquals(2, orderList.size());

        assertEquals("createdDate", orderList.get(0).getProperty());
        assertTrue(orderList.get(0).isAscending());

        assertEquals("lastModifiedDate", orderList.get(1).getProperty());
        assertFalse(orderList.get(1).isAscending());
    }

    @Test
    public void buildPageableSortingWithUnsupportedSorting() {
        RecordsQuery query = new RecordsQuery();
        query.setPage(new SkipPage(40, 20));
        query.setSortBy(Lists.newArrayList(
            new SortBy("cm:created", true),
            new SortBy("cm:unsupported", false)
        ));

        Pageable pageable = criteriaBuilder.buildPageable(query);
        Sort sort = pageable.getSort();
        List<Sort.Order> orderList = IteratorUtils.toList(sort.iterator());

        assertEquals(1, orderList.size());

        assertEquals("createdDate", orderList.get(0).getProperty());
        assertTrue(orderList.get(0).isAscending());
    }

    @Test
    public void buildPageableSortingWithAllUnsupportedSorting() {
        RecordsQuery query = new RecordsQuery();
        query.setPage(new SkipPage(40, 20));
        query.setSortBy(Lists.newArrayList(
            new SortBy("cm:unsupported1", true),
            new SortBy("cm:unsupported2", false)
        ));

        Pageable pageable = criteriaBuilder.buildPageable(query);
        Sort sort = pageable.getSort();
        List<Sort.Order> orderList = IteratorUtils.toList(sort.iterator());

        assertEquals(1, orderList.size());

        assertEquals("id", orderList.get(0).getProperty());
        assertTrue(orderList.get(0).isDescending());
    }

}
