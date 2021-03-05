package lc.core

import scala.io.Source.fromFile
import org.json4s.{DefaultFormats, JValue, JObject, JField}
import org.json4s.jackson.JsonMethods.parse
import scala.collection.immutable.HashMap

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
  private val captchaConfig = (configJson \ "captchas")
  val captchaConfigMap: Map[String,HashMap[String,List[String]]] = captchaConfig.values.asInstanceOf[Map[String, HashMap[String, List[String]]]]

  val supportedLevels: Set[String] = getAllValues(configJson, "supportedLevels")
  val supportedMedia: Set[String] = getAllValues(configJson, "supportedMedia")
  val supportedinputType: Set[String] = getAllValues(configJson, "supportedinputType")

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
