package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.Json;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.web.client.WebClient;
import io.vertx.junit5.Timeout;
import io.vertx.junit5.VertxExtension;
import io.vertx.junit5.VertxTestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Iterator;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


/***
 * This tests the following features:
 *  - services persisted over reboot
 *  - adding name to service at creation time
 *  - peristence of creation time of service
 *  - polling of status of service and persistence of result
 *
 *  These tests need some further work once the test database
 *  is initialised with services at setup (ie. it is in a known consistent state)
 */
@ExtendWith(VertxExtension.class)
public class TestMainVerticle {

  private static Logger log;

  @BeforeEach
  void deploy_verticle(Vertx vertx, VertxTestContext testContext) {
    vertx.deployVerticle(new MainVerticle(), testContext.succeeding(id -> testContext.completeNow()));
    log = Logger.getLogger("TestMainVerticle");
  }

  @Test
  @DisplayName("Start a web server on localhost responding to path /service on port 8080")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server(Vertx vertx, VertxTestContext testContext) {
    WebClient webClient = WebClient.create(vertx);
    webClient.get(8080, "::1", "/service")
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          response.result().bodyAsJsonArray()
              .stream()
              .forEach( o -> {
                log.info(((JsonObject)o).toString());
              });
          testContext.completeNow();
        }));
  }

  @Test
  @DisplayName("Start a web server on localhost and add a service")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server_add_service(Vertx vertx, VertxTestContext testContext) {
    JsonObject object = new JsonObject()
        .put("url", "https://www.kry.se")
        .put("name", "Kry Service");
    WebClient.create(vertx)
        .post(8080, "::1", "/service")
        .putHeader("Content-Type", "application/json")
        .putHeader("Content-Length", Integer.toString(object.toString().length()))
        .sendJson(object, response -> testContext.verify(() -> {
          assertEquals(201, response.result().statusCode());
          testContext.completeNow();
        }));
  }

  @Test
  @DisplayName("Start a web server on localhost, create a service then delete it")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server_create_delete_service(Vertx vertx, VertxTestContext testContext) {
    WebClient webClient = WebClient.create(vertx);
    addService(webClient, testContext, "https://www.kry.se", "Kry Service")
        .compose( newServiceId -> {
          return deleteService(webClient, testContext, newServiceId);
        })
        .compose(done -> {
          return Future.future(handle -> testContext.completeNow());
        });
  }

  @Test
  @DisplayName("Start a web server and poll until services status moves from failed")
  @Timeout(value = 100, timeUnit = TimeUnit.SECONDS)
  void start_http_server_and_poll(Vertx vertx, VertxTestContext testContext) {
    // server poll is once every 100s
    WebClient webClient = WebClient.create(vertx);
    vertx.setPeriodic(1000 * 10, timerId -> {
      webClient.get(8080, "::1", "/service")
          .send(response -> {
            assertEquals(200, response.result().statusCode());
            log.info("polling...");
            boolean isStatusChanged = response.result().bodyAsJsonArray()
                .stream()
                .anyMatch( o -> !((JsonObject)o).getString("status").equals("UNKNOWN"));
            if (isStatusChanged) {
              testContext.completeNow();
            }
          });
    });
  }

  @Test
  @DisplayName("Start a web server on localhost and delete the last service created")
  @Timeout(value = 10, timeUnit = TimeUnit.SECONDS)
  void start_http_server_delete_last_service(Vertx vertx, VertxTestContext testContext) {
    WebClient webClient = WebClient.create(vertx);
    // this relies on a service existing
    deleteLastService(webClient, testContext).setHandler( done -> testContext.completeNow());
  }

  private Future<Void> deleteLastService(WebClient webClient, VertxTestContext testContext) {
    Future<Void> future = Future.future();
    webClient.get(8080, "::1", "/service")
        .send(response -> testContext.verify(() -> {
          assertEquals(200, response.result().statusCode());
          JsonArray body = response.result().bodyAsJsonArray();
          assertTrue(body.size() > 0);
          JsonObject element = (JsonObject) body.getJsonObject(body.size()-1);
          webClient.delete(8080, "::1", "/service/" + element.getInteger("id"))
              .send(response2 -> testContext.verify(() -> {
                assertEquals(204, response2.result().statusCode());
                future.complete();
              }));
          }));
    return future;
  }

  private Future<Void> deleteService(WebClient webClient, VertxTestContext testContext, Integer id) {
    Future<Void> future = Future.future();
    webClient.delete(8080, "::1", "/service/" + id)
        .send(response2 -> testContext.verify(() -> {
          assertEquals(204, response2.result().statusCode());
          future.complete();
        }));
    return future;
  }

  private Future<Integer> addService(WebClient webClient, VertxTestContext testContext,
                                  String url, String name) {
    Future<Integer> future = Future.future();
    JsonObject object = new JsonObject()
        .put("url", url)
        .put("name", name);
    webClient.post(8080, "::1", "/service")
        .putHeader("Content-Type", "application/json")
        .putHeader("Content-Length", Integer.toString(object.toString().length()))
        .sendJson(object, response  -> {
          assertEquals(201, response.result().statusCode());
          String id = response.result().bodyAsString();
          future.complete(Integer.parseInt(id));
        });
    return future;
  }

}
