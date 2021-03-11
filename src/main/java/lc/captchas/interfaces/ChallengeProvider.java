package lc.captchas.interfaces;

import java.util.Map;
import java.util.List;

public interface ChallengeProvider {
  public String getId();

  public Challenge returnChallenge();

  public boolean checkAnswer(String secret, String answer);

  public void configure(String config);

  public Map<String, List<String>> supportedParameters();
}
