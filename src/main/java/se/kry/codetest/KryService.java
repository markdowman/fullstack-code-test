package se.kry.codetest;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.sql.Timestamp;

@Getter
@Setter
@AllArgsConstructor
public class KryService {
  private static final String DEFAULT_NAME = "default";

  private Integer id;
  private String url;
  private String name;
  private Timestamp added;
  private ServiceStatus status;

  public KryService(String url) {
    this.url = url;
    this.name = DEFAULT_NAME;
    this.added = new Timestamp(System.currentTimeMillis());
    this.status = ServiceStatus.UNKNOWN;
  }

  public KryService(String url, String name) {
    this.url = url;
    this.name = name;
    this.added = new Timestamp(System.currentTimeMillis());
    this.status = ServiceStatus.UNKNOWN;
  }
}
