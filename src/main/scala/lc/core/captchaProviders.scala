package lc.core

import lc.captchas.*
import lc.captchas.interfaces.ChallengeProvider
import lc.captchas.interfaces.Challenge
import scala.collection.mutable.Map
import lc.misc.HelperFunctions

class CaptchaProviders(config: Config) {
  private val providers = Map(
    "FilterChallenge" -> new FilterChallenge,
    // "FontFunCaptcha" -> new FontFunCaptcha,
    "PoppingCharactersCaptcha" -> new PoppingCharactersCaptcha,
    "ShadowTextCaptcha" -> new ShadowTextCaptcha,
    "RainDropsCaptcha" -> new RainDropsCP,
    "DebugCaptcha" -> new DebugCaptcha
    // "LabelCaptcha" -> new LabelCaptcha
  )

  def generateChallengeSamples(): Map[String, Challenge] = {
    providers.map { case (key, provider) =>
      (key, provider.returnChallenge("easy", "350x100"))
    }
  }

  private val captchaConfig = config.captchaConfig

  def getProviderById(id: String): ChallengeProvider = {
    return providers(id)
  }

  private def filterProviderByParam(param: Parameters): Iterable[(String, String)] = {
    val configFilter = for {
      configValue <- captchaConfig
      if configValue.allowedLevels.contains(param.level)
      if configValue.allowedMedia.contains(param.media)
      if configValue.allowedInputType.contains(param.input_type)
      if configValue.allowedSizes.contains(param.size)
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

  def getProvider(param: Parameters): Option[ChallengeProvider] = {
    val providerConfig = filterProviderByParam(param).toList
    if (providerConfig.nonEmpty) {
      val randomIndex = HelperFunctions.randomNumber(providerConfig.length)
      val providerIndex = providerConfig(randomIndex)._1
      val selectedProvider = providers(providerIndex)
      selectedProvider.configure(providerConfig(randomIndex)._2)
      Some(selectedProvider)
    } else {
      None
    }
  }
}
