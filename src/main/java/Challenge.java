package lc;

public class Challenge {
  public final byte[] content;
  public final String contentType;
  public final String secret;

  public Challenge(final byte[] content, final String contentType, final String secret) {
    this.content = content;
    this.contentType = contentType;
    this.secret = secret;
  }
}
