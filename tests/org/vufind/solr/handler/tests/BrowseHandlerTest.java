/**
 * This is a starting point for testing the component with embedded solr!
 * [Taken from https://stackoverflow.com/questions/45506381/how-to-debug-solr-plugin ]
 */

package org.vufind.solr.handler.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.System;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrRequest;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.QueryResponseWriter;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.response.XMLResponseWriter;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

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
    
    /*
     * PREPARE AND TEAR DOWN FOR TESTS
     */
    @BeforeClass
    public static void prepareClass() throws Exception
    {
        String solrHomeProp = "solr.solr.home";
        System.out.println(solrHomeProp + "= " + System.getProperty(solrHomeProp));
        // create the core container from the solr.home system property
        logger.info("Loading Solr container...");
        container = new CoreContainer();
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
    @Ignore
    @Test
    public void testBrowseHandler()
    {
        /* EXPLORE */
        org.apache.solr.core.PluginBag<SolrRequestHandler> handlerBag = bibCore.getRequestHandlers();
        Map<String,org.apache.solr.core.PluginBag.PluginHolder<SolrRequestHandler>> registry = handlerBag.getRegistry();
        Set<String> keys = registry.keySet();
        logger.info("Printing SolrRequestHandlers, count: " + keys.size());
        for (String k : keys) {
            logger.info("key: " + k);
        }

        /* PREPARE */
        /* Example param string:
         * json.nl=arrarr&offset=0&extras=author:format:publishDate&from=abu+nazzarah&source=title&rows=60&wt=json
         */
        //SolrRequestHandler browseHandler = bibCore.getRequestHandler(browseHandlerName);
        SolrQueryResponse response = new SolrQueryResponse();
        NamedList params = new NamedList();
        params.add("json.nl", "arrarr");
        params.add("wt","json");
        params.add("offset","0");
        params.add("rows","5");
        params.add("source","title");
        params.add("from","a");
        params.add("extras","author:format:publishDate");

        /* RUN */
        // do something with your search component
/*        try (LocalSolrQueryRequest request =
                        new LocalSolrQueryRequest(bibCore,params);
                    PrintWriter outWriter = new PrintWriter(System.out)
                ) {
            browseHandler.handleRequest(request,response);
            QueryResponseWriter responseWriter = bibCore.getQueryResponseWriter(request);
            try {
                outWriter.write("Request response:\n");
                responseWriter.write(outWriter, request, response);
            } catch (java.io.IOException e) {
                logger.info("ERROR writing response: Cause: " + e.getCause() +
                            "; Message:" + e.getMessage());
            }
        }
*/
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