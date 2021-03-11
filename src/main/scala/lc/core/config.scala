package lc.core

import scala.io.Source.fromFile
import org.json4s.{DefaultFormats, JValue, JObject, JField, JString}
import org.json4s.jackson.JsonMethods.parse

object Config {

  implicit val formats: DefaultFormats.type = DefaultFormats

  private val configFile = fromFile("config.json")
  private val configString =
    try configFile.mkString
    finally configFile.close
  private val configJson = parse(configString)

  val port: Int = (configJson \ "port").extract[Int]
  val throttle: Int = (configJson \ "throttle").extract[Int]
  val seed: Int = (configJson \ "randomSeed").extract[Int]
  val captchaExpiryTimeLimit: Int = (configJson \ "captchaExpiryTimeLimit").extract[Int]
  val threadDelay: Int = (configJson \ "threadDelay").extract[Int]

  private val captchaConfigJson = (configJson \ "captchas")
  val captchaConfigTransform: JValue = captchaConfigJson transformField {
    case JField("config", JObject(config)) => ("config", JString(config.toString))
  }
  val captchaConfig: List[CaptchaConfig] = captchaConfigTransform.extract[List[CaptchaConfig]]
  val allowedLevels: Set[String] = getAllValues(configJson, ParametersEnum.ALLOWEDLEVELS.toString)
  val allowedMedia: Set[String] = getAllValues(configJson, ParametersEnum.ALLOWEDMEDIA.toString)
  val allowedInputType: Set[String] = getAllValues(configJson, ParametersEnum.ALLOWEDINPUTTYPE.toString)

  private def getAllValues(config: JValue, param: String): Set[String] = {
    val configValues = (config \\ param)
    val result = for {
      JObject(child) <- configValues
      JField(param) <- child
    } yield (param)

    var valueSet = Set[String]()
    for (valueList <- result) {
      for (value <- valueList._2.children) {
        valueSet += value.values.toString
      }
    }
    valueSet
  }

}
