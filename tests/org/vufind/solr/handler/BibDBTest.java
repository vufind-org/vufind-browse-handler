/**
 *
 */
package org.vufind.solr.handler;

import static org.junit.Assert.*;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.RefCounted;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * @author tod
 *
 */
public class BibDBTest
{
    private static CoreContainer container;
    private static SolrCore bibCore;
    // Must match the name the BrowseRequestHandler uses under the biblio core
    public static String browseHandlerName = "/browse";

    /*
     * PREPARE AND TEAR DOWN FOR TESTS
     */

    /**
     * @throws java.lang.Exception
     */
    @BeforeClass
    public static void setUpBeforeClass() throws Exception
    {
        String solrHomeProp = "solr.solr.home";
        Log.info("BibDBTest#setUpBeforeClass: %s = %s", solrHomeProp, System.getProperty(solrHomeProp));
        // create the core container from the solr.home system property
        Log.info("Loading Solr container...");
        container = new CoreContainer(Paths.get(System.getProperty(solrHomeProp)), new Properties());
        container.load();
        Log.info("Solr container loaded!");
        bibCore = container.getCore("biblio");
    }

    /**
     * @throws java.lang.Exception
     */
    @AfterClass
    public static void tearDownAfterClass() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @Before
    public void setUp() throws Exception
    {
    }

    /**
     * @throws java.lang.Exception
     */
    @After
    public void tearDown() throws Exception
    {
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#BibDB(org.apache.lucene.search.IndexSearcher, java.lang.String)}.
     */
    @Test
    public void testBibDB()
    {
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            // just a smoke test, confirm that constructor succeeds
            BibDB bibDb = new BibDB(searcher, "title_fullStr");
            assertNotNull(bibDb);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("Threw exception while instantiating BibDB object");
        } finally {
            searcherRef.decref();
        }
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#recordCount(java.lang.String)}.
     * <p>
     * Success relies on knowing how many matches are in the test data.
     */
    @Test
    public void testRecordCount()
    {
        String title = "A common title";
        int titleCount = 3;
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
        try {
            assertEquals(titleCount, bibDbForTitle.recordCount(title));
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            fail("recordCount(" + title + ") threw an exception");
        }
        searcherRef.decref();
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingIDs(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingIDs()
    {
        //Log.info("Entering testMatchingIDs");
        String title = "A common title";
        int idCount = 3;
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            List<String> ids = bibDbForTitle.matchingIDs(title, "id", 10).get("id")
                               .stream()
                               .flatMap(Collection::stream)
                               .collect(Collectors.toList());
            assertEquals(idCount, ids.size());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingExtras(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingExtras()
    {
        //Log.info("Entering testMatchingExtras");
        String title = "A common title";
        String extras = "id:format:author";
        int idCount = 3;
        int maxBibs = 10;
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            Map<String, List<Collection<String>>> extrasMap = bibDbForTitle.matchingExtras(title, extras, maxBibs);
            List<String> ids = extrasMap.get("id")
                               .stream()
                               .flatMap(Collection::stream)
                               .collect(Collectors.toList());
            //Log.info("extrasMap: %s", extrasMap);
            //Log.info("ids: %s", ids);
            assertEquals(idCount, ids.size());
            for (String f : extras.split(":")) {
                assertNotNull(String.format("No map entry for extra field ", f),extrasMap.get(f));
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingExtras(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingExtras_noLimit()
    {
        //Log.info("Entering testMatchingExtras_noLimit");
        String title = "A common title";
        int idCount = 3;
        int maxBibs = 0; // Read all of the matching records
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            List<String> ids = bibDbForTitle.matchingExtras(title, "id", maxBibs).get("id")
                               .stream()
                               .flatMap(Collection::stream)
                               .collect(Collectors.toList());
            assertEquals(idCount, ids.size());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingExtras(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingExtras_smallLimit()
    {
        //Log.info("Entering testMatchingExtras_smallLimit");
        String title = "A common title";
        int maxBibs = 1; // Tiny limit on matching records
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            List<String> ids = bibDbForTitle.matchingExtras(title, "id", maxBibs).get("id")
                               .stream()
                               .flatMap(Collection::stream)
                               .collect(Collectors.toList());
            assertEquals(maxBibs, ids.size());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingExtras(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingExtras_noExtras()
    {
        //Log.info("Entering testMatchingExtras_noExtras");
        String title = "A common title";
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            assertNull(bibDbForTitle.matchingExtras(title, null, 0));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingFields(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingFields()
    {
        //Log.info("Entering testMatchingExtras");
        String title = "A common title";
        String extras = "id:format:author";
        int idCount = 3;
        int maxBibs = 10;
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            Map<String, Collection<String>> extrasMap = bibDbForTitle.matchingFields(title, extras, maxBibs);
            Collection<String> ids = extrasMap.get("id");
            //Log.info("extrasMap: %s", extrasMap);
            //Log.info("ids: %s", ids);
            assertEquals(idCount, ids.size());
            for (String f : extras.split(":")) {
                assertNotNull(String.format("No map entry for extra field ", f),extrasMap.get(f));
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }


    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingFields(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingFields_noLimit()
    {
        //Log.info("Entering testMatchingFields_noLimit");
        String title = "A common title";
        int idCount = 3;
        int maxBibs = 0; // Read all of the matching records
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            Collection<String> ids = bibDbForTitle.matchingFields(title, "id", maxBibs).get("id");
            assertEquals(idCount, ids.size());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingFields(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingFields_smallLimit()
    {
        //Log.info("Entering testMatchingFields_smallLimit");
        String title = "A common title";
        int maxBibs = 1; // Tiny limit on matching records
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            Collection<String> ids = bibDbForTitle.matchingFields(title, "id", maxBibs).get("id");
            assertEquals(maxBibs, ids.size());
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }

    /**
     * Test method for {@link org.vufind.solr.handler.BibDB#matchingFields(java.lang.String, java.lang.String, int)}.
     */
    @Test
    public void testMatchingFields_noFields()
    {
        //Log.info("Entering testMatchingFields_noFields");
        String title = "A common title";
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        try {
            BibDB bibDbForTitle = new BibDB(searcher, "title_fullStr");
            assertNull(bibDbForTitle.matchingFields(title, null, 0));
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } finally {
            searcherRef.decref();
        }
    }
}
