package lc.core

import scala.io.Source.fromFile
import zio.blocks.schema._
import zio.blocks.schema.json._
import java.io.{FileNotFoundException, File, PrintWriter}
import java.{util => ju}
import lc.misc.HelperFunctions
import java.nio.ByteBuffer

class Config(configFilePath: String) {

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

  private val appConfigEither = AppConfig.codec.decode(ByteBuffer.wrap(configString.getBytes("UTF-8")))
  private val appConfig = appConfigEither match {
    case Right(conf) => conf
    case Left(err) => throw new Exception(err.toString)
  }
  private val configFields: ConfigField = appConfig.toConfigField

  val port: Int = configFields.portInt.getOrElse(8888)
  val address: String = configFields.address.getOrElse("0.0.0.0")
  val bufferCount: Int = configFields.bufferCountInt.getOrElse(1000)
  val seed: Int = configFields.seedInt.getOrElse(375264328)
  val captchaExpiryTimeLimit: Int = configFields.captchaExpiryTimeLimitInt.getOrElse(5)
  val threadDelay: Int = configFields.threadDelayInt.getOrElse(2)
  val playgroundEnabled: Boolean = configFields.playgroundEnabledBool.getOrElse(true)
  val corsHeader: String = configFields.corsHeader.getOrElse("")
  val maxAttempts: Int = Math.max(1, (configFields.maxAttemptsRatioFloat.getOrElse(0.01f) * bufferCount).toInt)

  val captchaConfig: List[CaptchaConfig] = appConfig.captchas
  val allowedLevels: Set[String] = captchaConfig.flatMap(_.allowedLevels).toSet
  val allowedMedia: Set[String] = captchaConfig.flatMap(_.allowedMedia).toSet
  val allowedInputType: Set[String] = captchaConfig.flatMap(_.allowedInputType).toSet

  HelperFunctions.setSeed(seed)

  private def getDefaultConfig(): String = {
    val defaultConfig = AppConfig(
      seed = Some(new ju.Random().nextInt()),
      port = Some(8888),
      address = Some("0.0.0.0"),
      captchaExpiryTimeLimit = Some(5),
      bufferCount = Some(1000),
      threadDelay = Some(2),
      playgroundEnabled = Some(true),
      corsHeader = Some(""),
      maxAttemptsRatio = Some(0.01f),
      captchas = List(
        CaptchaConfig(
          name = "FilterChallenge",
          allowedLevels = List("medium", "hard"),
          allowedMedia = List("image/png"),
          allowedInputType = List("text"),
          allowedSizes = List("350x100"),
          config = Json.Object()
        ),
        CaptchaConfig(
          name = "PoppingCharactersCaptcha",
          allowedLevels = List("hard"),
          allowedMedia = List("image/gif"),
          allowedInputType = List("text"),
          allowedSizes = List("350x100"),
          config = Json.Object()
        ),
        CaptchaConfig(
          name = "ShadowTextCaptcha",
          allowedLevels = List("easy"),
          allowedMedia = List("image/png"),
          allowedInputType = List("text"),
          allowedSizes = List("350x100"),
          config = Json.Object()
        ),
        CaptchaConfig(
          name = "RainDropsCaptcha",
          allowedLevels = List("easy", "medium"),
          allowedMedia = List("image/gif"),
          allowedInputType = List("text"),
          allowedSizes = List("350x100"),
          config = Json.Object()
        ),
        CaptchaConfig(
          name = "CrumpledTextCaptcha",
          allowedLevels = List("easy", "medium", "hard"),
          allowedMedia = List("image/png"),
          allowedInputType = List("text"),
          allowedSizes = List("350x100"),
          config = Json.Object()
        )
      )
    )

    new String(BufferEncoder.encode(defaultConfig, AppConfig.codec), "UTF-8")
  }

}

object Config {
}
