package iudx.resource.server.databroker;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.ext.web.client.WebClientOptions;
import io.vertx.pgclient.PgConnectOptions;
import io.vertx.pgclient.PgPool;
import io.vertx.rabbitmq.RabbitMQClient;
import io.vertx.rabbitmq.RabbitMQOptions;
import io.vertx.serviceproxy.ServiceBinder;
import io.vertx.sqlclient.PoolOptions;

/**
 * The Data Broker Verticle.
 * <h1>Data Broker Verticle</h1>
 * <p>
 * The Data Broker Verticle implementation in the the IUDX Resource Server exposes the
 * {@link iudx.resource.server.databroker.DataBrokerService} over the Vert.x Event Bus.
 * </p>
 * 
 * @version 1.0
 * @since 2020-05-31
 */

public class DataBrokerVerticle extends AbstractVerticle {

  private static final String BROKER_SERVICE_ADDRESS = "iudx.rs.broker.service";
  private static final Logger LOGGER = LogManager.getLogger(DataBrokerVerticle.class);
  private DataBrokerService databroker;
  private RabbitMQOptions config;
  private RabbitMQClient client;
  private String dataBrokerIP;
  private int dataBrokerPort;
  private int dataBrokerManagementPort;
  private String dataBrokerVhost;
  private String dataBrokerUserName;
  private String dataBrokerPassword;
  private int connectionTimeout;
  private int requestedHeartbeat;
  private int handshakeTimeout;
  private int requestedChannelMax;
  private int networkRecoveryInterval;
  private WebClient webClient;
  private WebClientOptions webConfig;
  /* Database Properties */
  private String databaseIP;
  private int databasePort;
  private String databaseName;
  private String databaseUserName;
  private String databasePassword;
  private int poolSize;
  private int databasePoolSize;
  private PgPool pgclient;
  private PoolOptions poolOptions;
  private PgConnectOptions connectOptions;
  private RabbitClient rabbitClient;
  private RabbitWebClient rabbitWebClient;
  private PostgresClient pgClient;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   */

  @Override
  public void start() throws Exception {

    /* Read the configuration and set the rabbitMQ server properties. */
    dataBrokerIP = config().getString("dataBrokerIP");
    dataBrokerPort = Integer.parseInt(config().getString("dataBrokerPort"));
    dataBrokerManagementPort =
      Integer.parseInt(config().getString("dataBrokerManagementPort"));
    dataBrokerVhost = config().getString("dataBrokerVhost");
    dataBrokerUserName = config().getString("dataBrokerUserName");
    dataBrokerPassword = config().getString("dataBrokerPassword");
    connectionTimeout = Integer.parseInt(config().getString("connectionTimeout"));
    requestedHeartbeat = Integer.parseInt(config().getString("requestedHeartbeat"));
    handshakeTimeout = Integer.parseInt(config().getString("handshakeTimeout"));
    requestedChannelMax = Integer.parseInt(config().getString("requestedChannelMax"));
    networkRecoveryInterval =
      Integer.parseInt(config().getString("networkRecoveryInterval"));
    databaseIP = config().getString("callbackDatabaseIP");
    databasePort = Integer.parseInt(config().getString("callbackDatabasePort"));
    databaseName = config().getString("callbackDatabaseName");
    databaseUserName = config().getString("callbackDatabaseUserName");
    databasePassword = config().getString("callbackDatabasePassword");
    poolSize = Integer.parseInt(config().getString("callbackpoolSize"));

    /* Configure the RabbitMQ Data Broker client with input from config files. */

    config = new RabbitMQOptions();
    config.setUser(dataBrokerUserName);
    config.setPassword(dataBrokerPassword);
    config.setHost(dataBrokerIP);
    config.setPort(dataBrokerPort);
    config.setVirtualHost(dataBrokerVhost);
    config.setConnectionTimeout(connectionTimeout);
    config.setRequestedHeartbeat(requestedHeartbeat);
    config.setHandshakeTimeout(handshakeTimeout);
    config.setRequestedChannelMax(requestedChannelMax);
    config.setNetworkRecoveryInterval(networkRecoveryInterval);
    config.setAutomaticRecoveryEnabled(true);

    webConfig = new WebClientOptions();
    webConfig.setKeepAlive(true);
    webConfig.setConnectTimeout(86400000);
    webConfig.setDefaultHost(dataBrokerIP);
    webConfig.setDefaultPort(dataBrokerManagementPort);
    webConfig.setKeepAliveTimeout(86400000);

    /* Create a RabbitMQ Clinet with the configuration and vertx cluster instance. */

    client = RabbitMQClient.create(vertx, config);

    /* Create a Vertx Web Client with the configuration and vertx cluster instance. */

    webClient = WebClient.create(vertx, webConfig); 

    /* Set Connection Object */
    if (connectOptions == null) {
      connectOptions = new PgConnectOptions().setPort(databasePort).setHost(databaseIP)
        .setDatabase(databaseName).setUser(databaseUserName).setPassword(databasePassword);
    }

    /* Pool options */
    if (poolOptions == null) {
      poolOptions = new PoolOptions().setMaxSize(poolSize);
    }

    /* Create the client pool */
    pgclient = PgPool.pool(vertx, connectOptions, poolOptions);

    /* Create a Json Object for properties */

    JsonObject propObj = new JsonObject();

    propObj.put("userName", dataBrokerUserName);
    propObj.put("password", dataBrokerPassword);
    propObj.put("vHost", dataBrokerVhost);
    propObj.put("databaseIP", databaseIP);
    propObj.put("databasePort", databasePort);
    propObj.put("databaseName", databaseName);
    propObj.put("databaseUserName", databaseUserName);
    propObj.put("databasePassword", databasePassword);
    propObj.put("databasePoolSize", poolSize);

    /* Call the databroker constructor with the RabbitMQ client. */

    rabbitWebClient = new RabbitWebClient(vertx, webConfig, propObj);
    pgClient = new PostgresClient(vertx, connectOptions, poolOptions);
    rabbitClient =
        new RabbitClient(vertx, config, rabbitWebClient, pgClient);

    databroker = new DataBrokerServiceImpl(rabbitClient, pgClient, dataBrokerVhost);


    /* Publish the Data Broker service with the Event Bus against an address. */

    new ServiceBinder(vertx).setAddress(BROKER_SERVICE_ADDRESS)
      .register(DataBrokerService.class, databroker);

  }
}
