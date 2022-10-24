package lc.captchas.interfaces;

import java.util.List;
import java.util.Map;

public interface ChallengeProvider {
  public String getId();

  public Challenge returnChallenge(String level, String size);

  public boolean checkAnswer(String secret, String answer);

  public void configure(String config);

  public Map<String, List<String>> supportedParameters();
}
