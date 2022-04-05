package lc.core

import org.json4s.jackson.Serialization.write
import lc.core.Config.formats

trait ByteConvert { def toBytes(): Array[Byte] }
case class Size(height: Int, width: Int)
case class Parameters(level: String, media: String, input_type: String, size: String)
case class Id(id: String) extends ByteConvert { def toBytes(): Array[Byte] = { write(this).getBytes } }
case class Image(image: Array[Byte]) extends ByteConvert { def toBytes(): Array[Byte] = { image } }
case class Answer(answer: String, id: String)
case class Success(result: String) extends ByteConvert { def toBytes(): Array[Byte] = { write(this).getBytes } }
case class Error(message: String) extends ByteConvert { def toBytes(): Array[Byte] = { write(this).getBytes } }
case class CaptchaConfig(
    name: String,
    allowedLevels: List[String],
    allowedMedia: List[String],
    allowedInputType: List[String],
    allowedSizes: List[String],
    config: String
)
case class ConfigField(
    port: Option[Integer],
    address: Option[String],
    bufferCount: Option[Integer],
    seed: Option[Integer],
    captchaExpiryTimeLimit: Option[Integer],
    threadDelay: Option[Integer],
    playgroundEnabled: Option[java.lang.Boolean],
    corsHeader: Option[String],
    maxAttemptsRatio: Option[java.lang.Float]
) {
  lazy val portInt: Option[Int] = mapInt(port)
  lazy val bufferCountInt: Option[Int] = mapInt(bufferCount)
  lazy val seedInt: Option[Int] = mapInt(seed)
  lazy val captchaExpiryTimeLimitInt: Option[Int] = mapInt(captchaExpiryTimeLimit)
  lazy val threadDelayInt: Option[Int] = mapInt(threadDelay)
  lazy val maxAttemptsRatioFloat: Option[Float] = mapFloat(maxAttemptsRatio)
  lazy val playgroundEnabledBool: Option[Boolean] = playgroundEnabled.map(_ || true)

  private def mapInt(x: Option[Integer]): Option[Int] = {
    x.map(_ + 0)
  }
  private def mapFloat(x: Option[java.lang.Float]): Option[Float] = {
    x.map(_ + 0.0f)
  }
}
