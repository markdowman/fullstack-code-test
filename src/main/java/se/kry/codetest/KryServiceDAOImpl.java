package se.kry.codetest;

import io.vertx.core.Future;
import io.vertx.core.json.JsonArray;

import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class KryServiceDAOImpl implements KryServiceDAO {
  private DBConnector connector;

  @Override
  public void initialise(DBConnector connector) {
    this.connector = connector;
  }

  /***
   * create a new Kry Service in db
   * @param
   * @return id of newly created row
   */
  @Override
  public Future<Integer> create(KryService service) {
    String insertQuery = "INSERT INTO SERVICE (`url`,`name`,`added`, 'status') VALUES (?, ?, ?, ?);";
    JsonArray params = new JsonArray()
        .add(service.getUrl())
        .add(service.getName())
        .add(service.getAdded().toString())
        .add(service.getStatus());
    Future<Integer> future = Future.future();
    connector.query(insertQuery, params).setHandler(done -> {
      if (done.succeeded()) {
        // TODO: make this a bit cleaner, use compose?
        connector.query("SELECT last_insert_rowid();").setHandler(done2 -> {
          if (done2.succeeded()) {
            future.complete(done2.result().getResults().get(0).getInteger(0));
          } else {
            done2.cause().printStackTrace();
            future.fail(done2.cause().getMessage());
          }
        });
      } else {
        done.cause().printStackTrace();
        future.fail(done.cause().getMessage());
      }
    });
    return future;
  }

  @Override
  public Future<Void> delete(Integer krServiceId) {
    String deleteQuery = "DELETE FROM SERVICE WHERE ? = `id`;";
    Future<Void> future = Future.future();
    JsonArray params = new JsonArray().add(krServiceId);
    connector.query(deleteQuery, params).setHandler(done -> {
      if (done.succeeded()) {
        future.complete();
      } else {
        done.cause().printStackTrace();
        future.fail(done.cause());
      }
    });
    return future;
  }

  @Override
  public Future<Void> updateStatus(KryService service) {
    String updateQuery = "UPDATE SERVICE SET `status` = ? WHERE ? = `id`;";
    Future<Void> future = Future.future();
    JsonArray params = new JsonArray()
        .add(service.getStatus())
        .add(service.getId());
    connector.query(updateQuery, params).setHandler(done -> {
      if (done.succeeded()) {
        future.complete();
      } else {
        done.cause().printStackTrace();
        future.fail(done.cause());
      }
    });
    return future;
  }

  @Override
  public Future<List<KryService>> findServices() {
    String selectAllQuery = "SELECT * FROM SERVICE;";
    Future<List<KryService>> future = Future.future();
    List<KryService> serviceList = new ArrayList<>();
    connector.query(selectAllQuery).setHandler(done -> {
      if (done.succeeded()) {
          done.result().getResults().stream().forEach(s -> {
            try {
              serviceList.add(new KryService(
                  s.getInteger(0),
                  s.getString(1),
                  s.getString(2),
                  convertToTimestamp(s.getString(3)),
                  ServiceStatus.valueOf(s.getString(4))));
            } catch (ParseException e) {
              // TODO: handle this more elegantly
              e.printStackTrace();
            }
          });
          future.complete(serviceList);
      } else {
        done.cause().printStackTrace();
        future.fail(done.cause().getMessage());
      }
    });
    return future;
  }

  private Timestamp convertToTimestamp(String s) throws ParseException {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.SSS");
      Date parsedDate = dateFormat.parse(s);
      return new java.sql.Timestamp(parsedDate.getTime());
  }
}
