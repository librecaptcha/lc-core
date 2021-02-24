package lc.core

import lc.captchas._
import lc.captchas.interfaces.ChallengeProvider

object CaptchaProviders {
  private val providers = Map(
    "FilterChallenge" -> new FilterChallenge,
    //"FontFunCaptcha" -> new FontFunCaptcha,
    "GifCaptcha" -> new GifCaptcha,
    "ShadowTextCaptcha" -> new ShadowTextCaptcha,
    "RainDropsCaptcha" -> new RainDropsCP,
    //"LabelCaptcha" -> new LabelCaptcha
    )

  def generateChallengeSamples() = {
    providers.map {case (key, provider) =>
      (key, provider.returnChallenge())
    }
  }

  private val seed = System.currentTimeMillis.toString.substring(2,6).toInt
  private val random = new scala.util.Random(seed)

  private def getNextRandomInt(max: Int) = random.synchronized {
    random.nextInt(max)
  }

  def getProviderById(id: String): ChallengeProvider = {
    return providers(id)
  }
  
  def getProvider(): ChallengeProvider = {
    val keys = providers.keys
    val providerIndex = keys.toVector(getNextRandomInt(keys.size))
    providers(providerIndex)
  }
}