/**
 * 
 */
package org.vufind.solr.handler;

import static org.junit.Assert.*;

import java.io.IOException;
import java.util.logging.Logger;

import org.apache.lucene.search.IndexSearcher;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
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
public class BibDBTest {
    private static CoreContainer container;
    private static SolrCore bibCore;
    private static SolrCore authCore;
    private static SolrClient bibClient;
    private static SolrClient authClient;
    // Must match the name the BrowseRequestHandler uses under the biblio core
    public static String browseHandlerName = "/browse";

    private static final Logger logger = Logger.getGlobal();
    
    /*
     * PREPARE AND TEAR DOWN FOR TESTS
     */

	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
        String solrHomeProp = "solr.solr.home";
        System.out.println(solrHomeProp + "= " + System.getProperty(solrHomeProp));
        // create the core container from the solr.home system property
        logger.info("Loading Solr container...");
        container = new CoreContainer();
        container.load();
        logger.info("Solr container loaded!");
        authCore = container.getCore("authority");
        bibCore = container.getCore("biblio");
        authClient = new EmbeddedSolrServer(container, "authority");
        bibClient = new EmbeddedSolrServer(container, "biblio");
        
        
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for {@link org.vufind.solr.handler.BibDB#BibDB(org.apache.lucene.search.IndexSearcher, java.lang.String)}.
	 */
	@Test
	public void testBibDB() {
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
		try {
			// just a smoke test, confirm that constructor succeeds
			BibDB bibDb = new BibDB(searcher, "title");
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
	public void testRecordCount() {
		String title = "A common title".toLowerCase();
		int titleCount = 3;
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
        BibDB bibDbForTitle = new BibDB(searcher, "title");
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
	public void testMatchingIDs() {
		String title = "A common title".toLowerCase();
		int idCount = 3;
        RefCounted<SolrIndexSearcher> searcherRef = bibCore.getSearcher();
        IndexSearcher searcher = searcherRef.get();
		try {
			BibDB bibDbForTitle = new BibDB(searcher, "title");
			assertEquals(idCount, bibDbForTitle.matchingIDs(title, "id", 10).size());
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			searcherRef.decref();
		}
		fail("Not yet implemented");
	}

}
