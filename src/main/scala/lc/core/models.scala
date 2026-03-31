package lc.core

import com.github.plokhotnyuk.jsoniter_scala.macros._
import com.github.plokhotnyuk.jsoniter_scala.core._

trait ByteConvert { def toBytes(): Array[Byte] }
case class Size(height: Int, width: Int)

case class Parameters(level: String, media: String, input_type: String, size: String)
object Parameters {
  implicit val codec: JsonValueCodec[Parameters] = JsonCodecMaker.make
}

case class Id(id: String) extends ByteConvert { def toBytes(): Array[Byte] = { writeToArray(this) } }
object Id {
  implicit val codec: JsonValueCodec[Id] = JsonCodecMaker.make
}

case class Image(image: Array[Byte]) extends ByteConvert { def toBytes(): Array[Byte] = { image } }

case class Answer(answer: String, id: String)
object Answer {
  implicit val codec: JsonValueCodec[Answer] = JsonCodecMaker.make
}

case class Success(result: String) extends ByteConvert { def toBytes(): Array[Byte] = { writeToArray(this) } }
object Success {
  implicit val codec: JsonValueCodec[Success] = JsonCodecMaker.make
}

case class Error(message: String) extends ByteConvert { def toBytes(): Array[Byte] = { writeToArray(this) } }
object Error {
  implicit val codec: JsonValueCodec[Error] = JsonCodecMaker.make
}

case class JSONString(string: String)

object JSONString {
  implicit val codec: JsonValueCodec[JSONString] = new JsonValueCodec[JSONString] {
    def decodeValue(in: JsonReader, default: JSONString): JSONString = {
      val raw = in.readRawValAsBytes()
      JSONString(new String(raw, "UTF-8"))
    }

    def encodeValue(x: JSONString, out: JsonWriter): Unit = {
      out.writeRawVal(x.string.getBytes("UTF-8"))
    }

    def nullValue: JSONString = null.asInstanceOf[JSONString]
  }
}

case class CaptchaConfig(
    name: String,
    allowedLevels: List[String],
    allowedMedia: List[String],
    allowedInputType: List[String],
    allowedSizes: List[String],
    config: JSONString
)

case class AppConfig(
    port: Option[Int] = None,
    address: Option[String] = None,
    bufferCount: Option[Int] = None,
    seed: Option[Int] = None,
    captchaExpiryTimeLimit: Option[Int] = None,
    threadDelay: Option[Int] = None,
    playgroundEnabled: Option[Boolean] = None,
    corsHeader: Option[String] = None,
    maxAttemptsRatio: Option[Float] = None,
    captchas: List[CaptchaConfig] = List.empty
) {
  def toConfigField: ConfigField = ConfigField(
    port, address, bufferCount, seed, captchaExpiryTimeLimit,
    threadDelay, playgroundEnabled, corsHeader, maxAttemptsRatio
  )
}
object AppConfig {
  implicit val codec: JsonValueCodec[AppConfig] = JsonCodecMaker.make
}
case class ConfigField(
    port: Option[Int] = None,
    address: Option[String] = None,
    bufferCount: Option[Int] = None,
    seed: Option[Int] = None,
    captchaExpiryTimeLimit: Option[Int] = None,
    threadDelay: Option[Int] = None,
    playgroundEnabled: Option[Boolean] = None,
    corsHeader: Option[String] = None,
    maxAttemptsRatio: Option[Float] = None
) {
  lazy val portInt: Option[Int] = port
  lazy val bufferCountInt: Option[Int] = bufferCount
  lazy val seedInt: Option[Int] = seed
  lazy val captchaExpiryTimeLimitInt: Option[Int] = captchaExpiryTimeLimit
  lazy val threadDelayInt: Option[Int] = threadDelay
  lazy val maxAttemptsRatioFloat: Option[Float] = maxAttemptsRatio
  lazy val playgroundEnabledBool: Option[Boolean] = playgroundEnabled.map(_ || false)
}
