package se.kry.codetest;

import io.vertx.core.Future;

import java.util.List;

/***
 * DAO to abstract database operations for KryService
 */
public interface KryServiceDAO {
    void initialise(DBConnector connector);
    Future<Integer> create(KryService service);
    Future<Void> delete(Integer krServiceId);
    Future<List<KryService>> findServices();
    Future<Void> updateStatus(KryService service);
}
