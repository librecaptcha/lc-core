package lc.core

import scala.io.Source.fromFile
import org.json4s.{DefaultFormats, JValue, JObject, JField}
import org.json4s.jackson.JsonMethods.parse

object Config {

  implicit val formats: DefaultFormats.type = DefaultFormats

  private val configFile = fromFile("config.json")
  private val configString =
    try configFile.mkString
    finally configFile.close
  private val configJson = parse(configString)

  private val port = (configJson \ "port").extract[Int]
  private val throttle = (configJson \ "throttle").extract[Int]
  private val seed = (configJson \ "randomSeed").extract[Int]
  private val captchaExpiryTimeLimit = (configJson \ "captchaExpiryTimeLimit").extract[Int]
  private val threadDelay = (configJson \ "threadDelay").extract[Int]
  private val capthcaConfig = (configJson \ "captchas")

  private val supportedLevels = getAllValues(configJson, "supportedLevels")
  private val supportedMedia = getAllValues(configJson, "supportedMedia")
  private val supportedinputType = getAllValues(configJson, "supportedinputType")

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

  def getPort(): Int = {
    port
  }

  def getThrottle(): Int = {
    throttle
  }

  def getSeed(): Int = {
    seed
  }

  def getCaptchaExpiryTimeLimit(): Int = {
    captchaExpiryTimeLimit
  }

  def getThreadDelay(): Int = {
    threadDelay
  }

  def getCaptchaConfig(): JValue = {
    capthcaConfig
  }

  def getSupportedLevels(): Set[String] = {
    supportedLevels
  }

  def getSupportedMedia(): Set[String] = {
    supportedMedia
  }

  def getSupportedinputType(): Set[String] = {
    supportedinputType
  }

}
