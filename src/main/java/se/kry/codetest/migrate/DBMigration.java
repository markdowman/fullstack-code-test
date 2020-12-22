package se.kry.codetest.migrate;

import io.vertx.core.Vertx;
import se.kry.codetest.DBConnector;

public class DBMigration {

  public static void main(String[] args) {
    Vertx vertx = Vertx.vertx();
    DBConnector connector = new DBConnector(vertx);
    connector.query("CREATE TABLE IF NOT EXISTS service (" +
                    "id int(11) unsiqned NOT NULL auto_increment," +
                    "url VARCHAR(128) NOT NULL," +
                    "name VARCHAR(128) NOT NULL," +
                    "added NOT NULL," +
                    "status VARCHAR(128)").setHandler(done -> {
      if(done.succeeded()){
        System.out.println("completed db migrations");
      } else {
        done.cause().printStackTrace();
      }
      vertx.close(shutdown -> {
        System.exit(0);
      });
    });
  }
}
