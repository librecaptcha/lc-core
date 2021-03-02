package lc.core

import lc.captchas._
import lc.captchas.interfaces.ChallengeProvider
import lc.captchas.interfaces.Challenge
import org.json4s.{DefaultFormats, JObject, JField, JArray, JString}
import org.json4s.jackson.Serialization.write

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

  implicit val formats: DefaultFormats.type = DefaultFormats
  private val seed = Config.getSeed
  private val random = new scala.util.Random(seed)
  private val config = Config.getCaptchaConfig

  private def getNextRandomInt(max: Int): Int =
    random.synchronized {
      random.nextInt(max)
    }

  def getProviderById(id: String): ChallengeProvider = {
    return providers(id)
  }

  private def filterProviderByParam(param: Parameters): List[(String, String)] = {
    for {
      JObject(child) <- config
      JField("name", JString(name)) <- child
      JField("supportedLevels", JArray(supportedLevels)) <- child
      JField("supportedMedia", JArray(supportedMedia)) <- child
      JField("supportedinputType", JArray(supportedinputType)) <- child
      JField("config", config) <- child
      if supportedLevels.contains(JString(param.level))
      if supportedMedia.contains(JString(param.media))
      if supportedinputType.contains(JString(param.input_type))
    } yield (name, write(config))
  }

  def getProvider(param: Parameters): ChallengeProvider = {
    val providerConfig = filterProviderByParam(param)
    val randomIndex = getNextRandomInt(providerConfig.length)
    val providerIndex = providerConfig(randomIndex)._1
    val selectedProvider = providers(providerIndex)
    selectedProvider.configure(providerConfig(randomIndex)._2)
    selectedProvider
  }
}
