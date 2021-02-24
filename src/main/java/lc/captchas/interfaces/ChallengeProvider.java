package lc.captchas.interfaces;

public interface ChallengeProvider {
  public String getId();

  public Challenge returnChallenge();

  public boolean checkAnswer(String secret, String answer);

  // TODO: def configure(): Unit
}
