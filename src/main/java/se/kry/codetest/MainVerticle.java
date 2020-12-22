package se.kry.codetest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import io.vertx.ext.web.handler.StaticHandler;
import java.util.List;
import java.util.stream.Collectors;

public class MainVerticle extends AbstractVerticle {

  private DBConnector connector;
  private BackgroundPoller poller = new BackgroundPoller();
  private KryServiceDAO serviceDAO = new KryServiceDAOImpl();

  @Override
  public void start(Future<Void> startFuture) {
    connector = new DBConnector(vertx);
    serviceDAO.initialise(connector);
    Router router = Router.router(vertx);
    router.route().handler(BodyHandler.create());
    vertx.setPeriodic(1000 * 60, timerId -> poller.pollServices(vertx, serviceDAO));
    setRoutes(router);
    vertx
        .createHttpServer()
        .requestHandler(router)
        .listen(8080, result -> {
          if (result.succeeded()) {
            System.out.println("KRY code test service started");
            startFuture.complete();
          } else {
            startFuture.fail(result.cause());
          }
        });
  }

  private void setRoutes(Router router){
    router.route("/*").handler(StaticHandler.create());
    router.get("/service").handler(this::getServices);
    router.post("/service").handler(this::addService);
    router.delete("/service/:id").handler(this::deleteService);
  }

  private void getServices(RoutingContext req) {
    serviceDAO.findServices().setHandler( h -> {
      if (h.succeeded()) {
        List<KryService> services = h.result();
        List<JsonObject> jsonServices = services
            .stream()
            .map(service ->
                new JsonObject()
                    .put("name", service.getName())
                    .put("url", service.getUrl())
                    .put("status", service.getStatus()))
            .collect(Collectors.toList());
        req.response()
            .putHeader("content-type", "application/json")
            // .end(new JsonArray(jsonServices).encode());
            .end(Json.encodePrettily(h.result()));
      } else {
        req.response()
            .setStatusCode(500)
            .end(h.cause().getMessage());
      }
    });
  }

  private void addService(RoutingContext req) {
    JsonObject jsonBody = req.getBodyAsJson();
    KryService service = new KryService(jsonBody.getString("url"), jsonBody.getString(("name")));
    serviceDAO.create(service).setHandler( h -> {
      if (h.succeeded()) {
        req.response()
            .putHeader("content-type", "application/json")
            .setStatusCode(201)
            .end(Json.encodePrettily(h.result()));
      } else {
        req.response()
            .setStatusCode(500)
            .end(h.cause().getMessage());
      }
    });
  }

  private void deleteService(RoutingContext req) {
    String s = req.pathParam("id");
    Integer id = Integer.parseInt(s);
    serviceDAO.delete(Integer.parseInt(req.pathParam("id"))).setHandler( h -> {
      req.response()
          .putHeader("content-type", "text/plain")
          .setStatusCode(204)
          .end("OK");
    });
  }

}



