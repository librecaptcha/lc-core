package lc.core

import scala.io.Source.fromFile
import org.json4s.{DefaultFormats, JValue, JObject, JField, JString}
import org.json4s.jackson.JsonMethods.{parse, render, pretty}
import org.json4s.JsonDSL._
import java.io.{FileNotFoundException, File, PrintWriter}
import java.{util => ju}
import lc.misc.HelperFunctions

object Config {

  implicit val formats: DefaultFormats.type = DefaultFormats

  private val configFilePath = "data/config.json"
  private val configString =
    try {
      val configFile = fromFile(configFilePath)
      val configFileContent = configFile.mkString
      configFile.close
      configFileContent
    } catch {
      case _: FileNotFoundException => {
        val configFileContent = getDefaultConfig()
        val configFile = new PrintWriter(new File(configFilePath))
        configFile.write(configFileContent)
        configFile.close
        configFileContent
      }
      case exception: Exception => {
        println(exception.getStackTrace)
        throw new Exception(exception.getMessage)
      }
    }

  private val configJson = parse(configString)

  val port: Int = (configJson \ AttributesEnum.PORT.toString).extract[Int]
  val throttle: Int = (configJson \ AttributesEnum.THROTTLE.toString).extract[Int]
  val seed: Int = (configJson \ AttributesEnum.RANDOM_SEED.toString).extract[Int]
  val captchaExpiryTimeLimit: Int = (configJson \ AttributesEnum.CAPTCHA_EXPIRY_TIME_LIMIT.toString).extract[Int]
  val threadDelay: Int = (configJson \ AttributesEnum.THREAD_DELAY.toString).extract[Int]

  private val captchaConfigJson = (configJson \ "captchas")
  val captchaConfigTransform: JValue = captchaConfigJson transformField {
    case JField("config", JObject(config)) => ("config", JString(config.toString))
  }
  val captchaConfig: List[CaptchaConfig] = captchaConfigTransform.extract[List[CaptchaConfig]]
  val allowedLevels: Set[String] = captchaConfig.flatMap(_.allowedLevels).toSet
  val allowedMedia: Set[String] = captchaConfig.flatMap(_.allowedMedia).toSet
  val allowedInputType: Set[String] = captchaConfig.flatMap(_.allowedInputType).toSet

  HelperFunctions.seed = seed

  private def getDefaultConfig(): String = {
    val defaultConfigMap =
      (AttributesEnum.RANDOM_SEED.toString -> new ju.Random().nextInt()) ~
        (AttributesEnum.PORT.toString -> 8888) ~
        (AttributesEnum.CAPTCHA_EXPIRY_TIME_LIMIT.toString -> 5) ~
        (AttributesEnum.THROTTLE.toString -> 1000) ~
        (AttributesEnum.THREAD_DELAY.toString -> 2) ~
        ("captchas" -> List(
          (
            (AttributesEnum.NAME.toString -> "FilterChallenge") ~
              (ParametersEnum.ALLOWEDLEVELS.toString -> List("medium", "hard")) ~
              (ParametersEnum.ALLOWEDMEDIA.toString -> List("image/png")) ~
              (ParametersEnum.ALLOWEDINPUTTYPE.toString -> List("text")) ~
              (AttributesEnum.CONFIG.toString -> JObject())
          ),
          (
            (AttributesEnum.NAME.toString -> "PoppingCharactersCaptcha") ~
              (ParametersEnum.ALLOWEDLEVELS.toString -> List("hard")) ~
              (ParametersEnum.ALLOWEDMEDIA.toString -> List("image/gif")) ~
              (ParametersEnum.ALLOWEDINPUTTYPE.toString -> List("text")) ~
              (AttributesEnum.CONFIG.toString -> JObject())
          ),
          (
            (AttributesEnum.NAME.toString -> "ShadowTextCaptcha") ~
              (ParametersEnum.ALLOWEDLEVELS.toString -> List("easy")) ~
              (ParametersEnum.ALLOWEDMEDIA.toString -> List("image/png")) ~
              (ParametersEnum.ALLOWEDINPUTTYPE.toString -> List("text")) ~
              (AttributesEnum.CONFIG.toString -> JObject())
          ),
          (
            (AttributesEnum.NAME.toString -> "RainDropsCaptcha") ~
              (ParametersEnum.ALLOWEDLEVELS.toString -> List("easy", "medium")) ~
              (ParametersEnum.ALLOWEDMEDIA.toString -> List("image/gif")) ~
              (ParametersEnum.ALLOWEDINPUTTYPE.toString -> List("text")) ~
              (AttributesEnum.CONFIG.toString -> JObject())
          )
        ))

    pretty(render(defaultConfigMap))
  }

}
