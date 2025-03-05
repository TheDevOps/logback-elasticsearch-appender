package de.cgoit.logback.elasticsearch.it;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.MalformedURLException;

import org.apache.http.HttpHost;
import org.elasticsearch.client.RestClient;
import org.junit.After;
import org.junit.Before;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.elasticsearch.ElasticsearchContainer;
import org.testcontainers.utility.DockerImageName;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.ElasticsearchException;
import co.elastic.clients.elasticsearch._types.query_dsl.QueryBuilders;
import co.elastic.clients.elasticsearch.core.DeleteByQueryRequest;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.SearchResponse;
import co.elastic.clients.elasticsearch.core.search.HitsMetadata;
import co.elastic.clients.json.jackson.JacksonJsonpMapper;
import co.elastic.clients.transport.rest_client.RestClientTransport;
import de.cgoit.logback.elasticsearch.ElasticsearchAppender;

public abstract class IntegrationTest
{

    protected static final String ELASTICSEARCH_LOGGER_NAME = "ES_LOGGER";
    protected static final String ELASTICSEARCH_RAW_LOGGER_NAME = "ES_RAW_LOGGER";
    private static final Logger LOG = LoggerFactory.getLogger(IntegrationTest.class);
    private static final int WAIT_FOR_DOCUMENTS_MAX_RETRIES = 15;
    private static final int WAIT_FOR_DOCUMENTS_SLEEP_INTERVAL = 2000;
    private static final String ELASTICSEARCH_APPENDER_NAME = "ES_APPENDER";
    private static final String ELASTICSEARCH_RAW_APPENDER_NAME = "ES_RAW_APPENDER";

    protected static ElasticsearchClient client;
    protected static ElasticsearchContainer container;

    protected static void deleteAll() throws IOException
    {
        DeleteByQueryRequest request = new DeleteByQueryRequest.Builder()
            .index("_all")
            .query(QueryBuilders.matchAll().build()._toQuery())
            .build();
        client.deleteByQuery(request);
        LOG.info("Deleted all documents from Elasticsearch.");
    }

    private static void configureElasticSearchAppender(String loggerName, String appenderName)
        throws MalformedURLException
    {
        ch.qos.logback.classic.Logger logbackLogger =
            (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(loggerName);
        ElasticsearchAppender appender = (ElasticsearchAppender) logbackLogger.getAppender(appenderName);

        String containerUrl = HttpHost.create(container.getHttpHostAddress()).toURI() + "/_bulk";
        LOG.info("Configure appender {} to use {} as container address.", appenderName, containerUrl);
        appender.setUrl(containerUrl);
    }

    @Before
    public void setupElasticSearchContainer() throws IOException
    {
        // Create the Elasticsearch container.
        DockerImageName elasticImage = DockerImageName
            .parse("docker.porscheinformatik.com/docker-proxy-elastic/elasticsearch/elasticsearch:7.17.28")
            .asCompatibleSubstituteFor("docker.elastic.co/elasticsearch/elasticsearch");
        IntegrationTest.container = new ElasticsearchContainer(elasticImage);

        // Start the container. This step might take some time...
        container.start();

        // Create the Elasticsearch client.
        RestClient restClient = RestClient.builder(HttpHost.create(container.getHttpHostAddress())).build();
        RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
        IntegrationTest.client = new ElasticsearchClient(transport);

        configureElasticSearchAppender(ELASTICSEARCH_LOGGER_NAME, ELASTICSEARCH_APPENDER_NAME);
        configureElasticSearchAppender(ELASTICSEARCH_RAW_LOGGER_NAME, ELASTICSEARCH_RAW_APPENDER_NAME);

        deleteAll();
    }

    @After
    public void tearDownElasticSearchContainer()
    {
        // Stop the container.
        IntegrationTest.container.stop();
    }

    protected HitsMetadata<?> searchAll() throws IOException
    {
        SearchRequest request = new SearchRequest.Builder()
            .query(QueryBuilders.matchAll().build()._toQuery())
            .build();
        SearchResponse<?> response = client.search(request, Object.class);
        return response.hits();
    }

    protected void checkLogEntries(long desiredCount) throws IOException
    {
        LOG.info("Check if we have {} documents in Elasticsearch. Max retries: {}", desiredCount,
            WAIT_FOR_DOCUMENTS_MAX_RETRIES);
        int retries = WAIT_FOR_DOCUMENTS_MAX_RETRIES;
        long hitcount = 0;
        while (hitcount != desiredCount && retries-- > 0)
        {
            try
            {
                LOG.debug("Found {} documents. Desired count is {}. Retry...", hitcount, desiredCount);
                Thread.sleep(WAIT_FOR_DOCUMENTS_SLEEP_INTERVAL);
                HitsMetadata<?> hits = searchAll();
                hitcount = hits.total().value();
            }
            catch (InterruptedException | ElasticsearchException ex)
            {
                // just retrying
            }
        }

        LOG.debug("Found {} documents. Desired count is {}.", hitcount, desiredCount);
        assertEquals(String.format("Document count should be %s", desiredCount), desiredCount, hitcount);
    }
}
