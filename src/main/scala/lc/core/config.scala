package lc.core

import scala.io.Source.fromFile
import org.json4s.{DefaultFormats, JValue, JObject, JField, JString}
import org.json4s.jackson.JsonMethods.{parse, render, pretty}
import org.json4s.JsonDSL._
import org.json4s.StringInput
import org.json4s.jvalue2monadic
import org.json4s.jvalue2extractable
import java.io.{FileNotFoundException, File, PrintWriter}
import java.{util => ju}
import lc.misc.HelperFunctions

class Config(configFilePath: String) {

  import Config.formats

  private val configString =
    try {
      val configFile = fromFile(configFilePath)
      val configFileContent = configFile.mkString
      configFile.close
      configFileContent
    } catch {
      case _: FileNotFoundException => {
        val configFileContent = getDefaultConfig()
        val file = if (new File(configFilePath).isDirectory) {
          new File(configFilePath.concat("/config.json"))
        } else {
          new File(configFilePath)
        }
        val configFile = new PrintWriter(file)
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

  val port: Int = getOptionalConfigParam(AttributesEnum.PORT.toString, "8888").toInt
  val address: String = getOptionalConfigParam(AttributesEnum.ADDRESS.toString, "0.0.0.0")
  val throttle: Int = getOptionalConfigParam(AttributesEnum.THROTTLE.toString, "1000").toInt
  val seed: Int = getOptionalConfigParam(AttributesEnum.RANDOM_SEED.toString, "375264328").toInt
  val captchaExpiryTimeLimit: Int = getOptionalConfigParam(AttributesEnum.CAPTCHA_EXPIRY_TIME_LIMIT.toString, "5").toInt
  val threadDelay: Int = getOptionalConfigParam(AttributesEnum.THREAD_DELAY.toString, "2").toInt
  val playgroundEnabled: Boolean = getOptionalConfigParam(AttributesEnum.PLAYGROUND_ENABLED.toString, "true").toBoolean
  val corsHeader: String = getOptionalConfigParam(AttributesEnum.CORS_HEADER.toString, "")
  val maxAttempts: Int = getOptionalConfigParam(AttributesEnum.MAX_ATTEMPTS.toString, "10").toInt

  private val captchaConfigJson = (configJson \ "captchas")
  val captchaConfigTransform: JValue = captchaConfigJson transformField { case JField("config", JObject(config)) =>
    ("config", JString(config.toString))
  }
  val captchaConfig: List[CaptchaConfig] = captchaConfigTransform.extract[List[CaptchaConfig]]
  val allowedLevels: Set[String] = captchaConfig.flatMap(_.allowedLevels).toSet
  val allowedMedia: Set[String] = captchaConfig.flatMap(_.allowedMedia).toSet
  val allowedInputType: Set[String] = captchaConfig.flatMap(_.allowedInputType).toSet

  HelperFunctions.setSeed(seed)

  private def getOptionalConfigParam(paramName: String, defaultValue: String): String = {
    val param: Option[(String, JValue)] = (configJson findField({
      case JField(`paramName`, _) => true
      case _ => false
    }))

    if(param.isDefined){
      param.get._2.extract[String]
    } else {
      defaultValue
    }
  }

  private def getDefaultConfig(): String = {
    val defaultConfigMap =
      (AttributesEnum.RANDOM_SEED.toString -> new ju.Random().nextInt()) ~
        (AttributesEnum.PORT.toString -> 8888) ~
        (AttributesEnum.ADDRESS.toString -> "0.0.0.0") ~
        (AttributesEnum.CAPTCHA_EXPIRY_TIME_LIMIT.toString -> 5) ~
        (AttributesEnum.THROTTLE.toString -> 1000) ~
        (AttributesEnum.THREAD_DELAY.toString -> 2) ~
        (AttributesEnum.PLAYGROUND_ENABLED.toString -> "true") ~
        (AttributesEnum.CORS_HEADER.toString -> "") ~
        (AttributesEnum.MAX_ATTEMPTS.toString -> 10) ~
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

object Config {
  implicit val formats: DefaultFormats.type = DefaultFormats
}
