package iudx.resource.server.database.latest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.MessageConsumer;
import io.vertx.core.json.JsonObject;
import io.vertx.serviceproxy.ServiceBinder;
import iudx.resource.server.database.archives.Constants;

public class LatestVerticle extends AbstractVerticle {


  /**
   * The Latest Verticle.
   * <h1>Latest Verticle</h1>
   * <p>
   * The Database Verticle implementation in the the IUDX Resource Server exposes the
   * {@link iudx.resource.server.database.archives.DatabaseService} over the Vert.x Event Bus.
   * </p>
   *
   * @version 1.0
   * @since 2020-05-31
   */
  private LatestDataService latestData;
  private RedisClient redisClient;
  private JsonObject attributeList;
  private ServiceBinder binder;
  private MessageConsumer<JsonObject> consumer;

  /**
   * This method is used to start the Verticle. It deploys a verticle in a cluster, registers the
   * service with the Event bus against an address, publishes the service with the service discovery
   * interface.
   *
   * @throws Exception which is a start up exception.
   */

  @Override
  public void start() throws Exception {


    attributeList = config().getJsonObject("attributeList");
    redisClient = new RedisClient(vertx, config());
    binder = new ServiceBinder(vertx);
    latestData = new LatestDataServiceImpl(redisClient, attributeList);

    consumer =
        binder.setAddress(Constants.LATEST_DATA_SERVICE_ADDRESS)
            .register(LatestDataService.class, latestData);
  }

  @Override
  public void stop() {
    binder.unregister(consumer);
  }
}

