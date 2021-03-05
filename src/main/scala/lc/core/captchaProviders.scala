package lc.core

import lc.captchas._
import lc.captchas.interfaces.ChallengeProvider
import lc.captchas.interfaces.Challenge
import scala.collection.mutable.Map

object CaptchaProviders {
  private val providers = Map(
    "FilterChallenge" -> new FilterChallenge,
    //"FontFunCaptcha" -> new FontFunCaptcha,
    "GifCaptcha" -> new GifCaptcha,
    "ShadowTextCaptcha" -> new ShadowTextCaptcha,
    "RainDropsCaptcha" -> new RainDropsCP
    //"LabelCaptcha" -> new LabelCaptcha
  )

  def generateChallengeSamples(): Map[String, Challenge] = {
    providers.map {
      case (key, provider) =>
        (key, provider.returnChallenge())
    }
  }

  private val seed = Config.seed
  private val random = new scala.util.Random(seed)
  private val config = Config.captchaConfigMap

  private def getNextRandomInt(max: Int): Int =
    random.synchronized {
      random.nextInt(max)
    }

  def getProviderById(id: String): ChallengeProvider = {
    return providers(id)
  }

  private def filterProviderByParam(param: Parameters): Iterable[(List[String], List[String])] = {
    for {
      configValue <- config.values
      if configValue("supportedLevels").contains(param.level)
      if configValue("supportedMedia").contains(param.media)
      if configValue("supportedinputType").contains(param.input_type)
    } yield (configValue("name"), configValue("config"))
  }

  def getProvider(param: Parameters): ChallengeProvider = {
    val providerConfig = filterProviderByParam(param).toList
    val randomIndex = getNextRandomInt(providerConfig.length)
    val providerIndex = providerConfig(randomIndex)._1
    val selectedProvider = providers(providerIndex(0))
    selectedProvider.configure(providerConfig(randomIndex)._2(0))
    selectedProvider
  }
}
