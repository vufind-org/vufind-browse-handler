/**
 * This is a starting point for testing the component with embedded solr!
 * [Taken from https://stackoverflow.com/questions/45506381/how-to-debug-solr-plugin ]
 */

package org.vufind.solr.handler.tests;

import static org.junit.Assert.*;

import java.io.IOException;
import java.lang.System;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.vufind.solr.handler.BrowseItem;
import org.vufind.solr.handler.BrowseList;
import org.vufind.solr.handler.MatchTypeResponse.MatchType;
import org.vufind.solr.handler.client.solrj.BrowseRequest;
import org.vufind.solr.handler.client.solrj.BrowseResponse;

public class BrowseHandlerTest
{
    private static CoreContainer container;
    private static SolrCore bibCore;
    private static SolrCore authCore;
    private static SolrClient bibClient;
    private static SolrClient authClient;
    // Must match the name the BrowseRequestHandler uses under the biblio core
    public static String browseHandlerName = "/browse";

    private static final Logger logger = Logger.getGlobal();

    public static void checkLogger()
    {
        System.out.println(String.format("#### checkLogger: logger = %s", logger));
    }
    /*
     * PREPARE AND TEAR DOWN FOR TESTS
     */
    @BeforeClass
    public static void prepareClass() throws Exception
    {
        checkLogger();

        String solrHomeProp = "solr.solr.home";
        System.out.println(solrHomeProp + "= " + System.getProperty(solrHomeProp));
        // create the core container from the solr.home system property
        logger.info("Loading Solr container...");
        container = new CoreContainer(Paths.get(System.getProperty(solrHomeProp)), new Properties());
        container.load();
        logger.info("Solr container loaded!");
        authCore = container.getCore("authority");
        bibCore = container.getCore("biblio");
        //logger.info("Solr cores loaded!");
        authClient = new EmbeddedSolrServer(container, "authority");
        bibClient = new EmbeddedSolrServer(container, "biblio");
        //logger.info("Solr clients loaded!");
    }

    @AfterClass
    public static void cleanUpClass()
    {
        try {
            authClient.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        try {
            bibClient.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        //authCore.close();
        //bibCore.close();
        container.shutdown();
        logger.info("Solr cores shut down!");
    }

    /* TESTS TO RUN */

    /**
     * Smoke test
     */
    @Test
    public void testBrowseHandler_smoke()
    {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("source", "title").add("from","Random title").add("rows","1");
        try {
            BrowseRequest req = new BrowseRequest(params);
            BrowseResponse res = req.process(bibClient);
            NamedList<Object> response = res.getResponse();
            logger.info("Response: " + response.toString());
        } catch (SolrServerException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
    /**
     * Test the search component here or just trigger it to debug
     */
    @Test
    public void testBrowseHandler()
    {
        /* EXPLORE */
        /*
        org.apache.solr.core.PluginBag<SolrRequestHandler> handlerBag = bibCore.getRequestHandlers();
        Map<String,org.apache.solr.core.PluginBag.PluginHolder<SolrRequestHandler>> registry = handlerBag.getRegistry();
        Set<String> keys = registry.keySet();
        logger.info("Printing SolrRequestHandlers, count: " + keys.size());
        for (String k : keys) {
            logger.info("key: " + k);
        }
        */

        /* PREPARE */
        /* Example param string:
         * json.nl=arrarr&offset=0&extras=author:format:publishDate&from=abu+nazzarah&source=title&rows=60&wt=json
         */
        int offset = 0;
        int rows = 5;
        String extras = "author:format:publishDate";

        NamedList<String> paramList = new NamedList<String>();
        paramList.add("json.nl", "arrarr");
        paramList.add("wt","json");
        paramList.add("offset", Integer.toString(offset));
        paramList.add("rows", Integer.toString(rows));
        paramList.add("source","title");
        paramList.add("from","a");
        paramList.add("extras", extras);
        SolrParams params = SolrParams.toSolrParams(paramList);

        /* RUN */
        // do something with your search component
        try {
            BrowseRequest req = new BrowseRequest(params);
            BrowseResponse res = req.process(bibClient);
            NamedList<Object> response = res.getResponse();
            logger.info("Response: " + response.toString());

            @SuppressWarnings("unchecked")
            Map<String,Object> responseBrowse = (Map<String, Object>) response.get("Browse");
            //assertEquals(Integer.toString(offset), responseBrowse.get("offset"));
            assertTrue(offset == (int) responseBrowse.get("offset"));

            int totalCount = (int) responseBrowse.get("totalCount");
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> itemList = (List<Map<String, Object>>) responseBrowse.get("items");
            BrowseList items = new BrowseList(itemList, totalCount);
            assertTrue(String.format("Expected BrowseList of %d items, but %d were returned", rows, items.size()),
                       rows == items.size());
            for (BrowseItem item : items) {
                Map<String, List<Collection<String>>> extrasMap = item.getExtras();
                for (String field : extras.split(":")) {
                    assertNotNull(String.format("missing field %s in item extras: %s", field, item),
                                  extrasMap.get(field));
                }
            }
        } catch (SolrServerException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        /* CHECK */
        // check results with asserts :)
    }

    @Test
    public void testBrowseHandler_HeadOfStringMatch()
    {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("source", "author").add("from","Triggs").add("rows","1");
        try {
            BrowseRequest req = new BrowseRequest(params);
            BrowseResponse res = req.process(bibClient);
            NamedList<Object> response = res.getResponse();
            logger.info("Response: " + response.toString());
            assertEquals(res.getMatchType(), MatchType.HEAD_OF_STRING.toString());
        } catch (SolrServerException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testBrowseHandler_ExactMatch()
    {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("source", "author").add("from","Triggs, Mark").add("rows","1");
        try {
            BrowseRequest req = new BrowseRequest(params);
            BrowseResponse res = req.process(bibClient);
            NamedList<Object> response = res.getResponse();
            logger.info("Response: " + response.toString());
            assertEquals(res.getMatchType(), MatchType.EXACT.toString());
        } catch (SolrServerException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    @Test
    public void testBrowseHandler_NoMatch()
    {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("source", "title").add("from","AAZZXX").add("rows","1");
        try {
            BrowseRequest req = new BrowseRequest(params);
            BrowseResponse res = req.process(bibClient);
            NamedList<Object> response = res.getResponse();
            logger.info("Response: " + response.toString());
            assertEquals(res.getMatchType(), MatchType.NONE.toString());
        } catch (SolrServerException | IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

}