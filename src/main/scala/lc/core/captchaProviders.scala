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
  private val config = Config.captchaConfig

  private def getNextRandomInt(max: Int): Int =
    random.synchronized {
      random.nextInt(max)
    }

  def getProviderById(id: String): ChallengeProvider = {
    return providers(id)
  }

  private def filterProviderByParam(param: Parameters): Iterable[(String, String)] = {
    val configFilter = for {
      configValue <- config
      if configValue.allowedLevels.contains(param.level)
      if configValue.allowedMedia.contains(param.media)
      if configValue.allowedInputType.contains(param.input_type)
    } yield (configValue.name, configValue.config)

    val providerFilter = for {
      providerValue <- configFilter
      providerConfigMap = providers(providerValue._1).supportedParameters()
      if providerConfigMap.get(ParametersEnum.SUPPORTEDLEVEL.toString).contains(param.level)
      if providerConfigMap.get(ParametersEnum.SUPPORTEDMEDIA.toString).contains(param.media)
      if providerConfigMap.get(ParametersEnum.SUPPORTEDINPUTTYPE.toString).contains(param.input_type)
    } yield (providerValue._1, providerValue._2)

    providerFilter
  }

  def getProvider(param: Parameters): ChallengeProvider = {
    val providerConfig = filterProviderByParam(param).toList
    if (providerConfig.length == 0) throw new NoSuchElementException(ErrorMessageEnum.NO_CAPTCHA.toString)
    val randomIndex = getNextRandomInt(providerConfig.length)
    val providerIndex = providerConfig(randomIndex)._1
    val selectedProvider = providers(providerIndex)
    selectedProvider.configure(providerConfig(randomIndex)._2)
    selectedProvider
  }
}
