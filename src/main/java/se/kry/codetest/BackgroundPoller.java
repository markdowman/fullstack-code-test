package se.kry.codetest;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonArray;
import io.vertx.ext.web.client.WebClient;

import java.security.Provider;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class BackgroundPoller {

  public Future<Void> pollServices(Vertx vertx, final KryServiceDAO kryServiceDAO) {
    Future<Void> future = Future.future();
    kryServiceDAO.findServices().setHandler(done -> {
      List<Future> futureList = new ArrayList<>();
      done.result().stream().forEach(service -> {
        futureList.add(updateServiceStatus(vertx, service, kryServiceDAO));
      });
      CompositeFuture.all(futureList);
    });
    return future;
  }

  private Future<Void> updateServiceStatus(Vertx vertx, KryService service, KryServiceDAO kryServiceDAO) {
    Future<Void> future = Future.future();
    WebClient.create(vertx)
        .get(service.getUrl())
        .send(response -> {
              if (response.succeeded() && response.result().body().toString().equals("OK")) {
                service.setStatus(ServiceStatus.OK);
              } else {
                service.setStatus(ServiceStatus.FAIL);
              }
              kryServiceDAO.updateStatus(service).setHandler(done -> {
                if (done.failed()) {
                  done.cause().printStackTrace();
                }
              });
            });
    return future;
  }
}
