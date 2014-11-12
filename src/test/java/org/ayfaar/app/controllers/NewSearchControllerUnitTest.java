package org.ayfaar.app.controllers;

import org.ayfaar.app.controllers.search.SearchCache;
import org.ayfaar.app.controllers.search.SearchQuotesHelper;
import org.ayfaar.app.controllers.search.SearchResultPage;
import org.ayfaar.app.dao.CommonDao;
import org.ayfaar.app.dao.SearchDao;
import org.ayfaar.app.dao.TermMorphDao;
import org.ayfaar.app.model.Term;
import org.ayfaar.app.model.TermMorph;
import org.ayfaar.app.utils.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.List;

import static java.util.Arrays.asList;
import static org.ayfaar.app.utils.TermsMap.TermProvider;
import static org.ayfaar.app.controllers.NewSearchController.PAGE_SIZE;
import static org.ayfaar.app.utils.CollectionUtils.transform;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.anyList;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class NewSearchControllerUnitTest {

    @Mock SearchDao searchDao;
    @Mock CommonDao commonDao;
    @Mock TermMorphDao termMorphDao;
    @Mock SearchCache cache;
    @Mock SearchQuotesHelper handleItems;
    //@Mock AliasesMap aliasesMap;
    @Mock TermsMap termsMap;
    @InjectMocks @Spy
    NewSearchController controller;


    @Before
    public void setUp() {
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testHasMore() {
        NewAliasesMap aliasesMap = new NewAliasesMap();
        String q = "время";
        TermProvider provider = aliasesMap.new TermProviderImpl(q, null, false);

        when(termsMap.getTermProvider(q)).thenReturn(provider);

        List<String> morphs = asList(q);
        when(controller.getAllMorphs(anyList())).thenReturn(morphs);

        List items = mock(List.class);
        when(searchDao.findInItems(morphs, 0, PAGE_SIZE+1, null)).thenReturn(items);
        when(items.size()).thenReturn(21);

        SearchResultPage page = (SearchResultPage) controller.search(q, 0, null);
        assertTrue(page.isHasMore());
        verify(searchDao, only()).findInItems(anyList(), anyInt(), anyInt(), anyString());
        verify(items).remove(20);
    }

    @Test
    public void testSearchPhrase() {
        String phrase = "каждый момент";

        controller.search(phrase, 0, null);
        verify(searchDao, only()).findInItems(asList(phrase), 0, PAGE_SIZE + 1, null);
    }

    @Test
    public void testGetAllMorphs() {
        final String t1 = "Трансгрессионная диверсификация";
        List<String> termNames = asList(t1, "Ирркогликтивная сингуляция");

        final NewAliasesMap map = new NewAliasesMap();
        List<TermProvider> providers = transform(termNames, new Transformer() {
            @Override
            public Object transform(Object value) {
                TermProvider provider = map.new TermProviderImpl(
                        UriGenerator.generate(Term.class, (String) value), null, false);
                return provider;
            }
        });

        when(termMorphDao.getList(anyString(), anyString())).then(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                String uri = (String) invocation.getArguments()[1];
                String name = UriGenerator.getValueFromUri(Term.class, uri);
                return asList(new TermMorph(name+1, uri), new TermMorph(name+2, uri));
            }
        });
        List<String> morphs = controller.getAllMorphs(providers);
        verify(termMorphDao, times(2)).getList(anyString(), anyString());
        assertEquals(6, morphs.size());
        assertTrue(morphs.contains(t1));
        assertTrue(morphs.contains(t1 + 1));
        assertTrue(morphs.contains(t1 + 2));
    }
}
