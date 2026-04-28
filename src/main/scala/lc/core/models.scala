package lc.core

import zio.blocks.schema._
import zio.blocks.schema.json._
import zio.blocks.schema.codec.BinaryCodec
import java.nio.ByteBuffer

trait ByteConvert { def toBytes(): Array[Byte] }
case class Size(height: Int, width: Int)

case class Parameters(level: String, media: String, input_type: String, size: String)
object Parameters {
  implicit val schema: Schema[Parameters] = Schema.derived
  implicit val codec: BinaryCodec[Parameters] = schema.derive(JsonFormat.deriver)
}

object BufferEncoder {
  def encode[A](value: A, codec: BinaryCodec[A]): Array[Byte] = {
    // Start with 1KB, if it fails, try with 10KB, 100KB, etc up to 1MB
    var size = 1024
    var result: Array[Byte] = null
    while (result == null && size <= 1048576) {
      try {
        val buf = ByteBuffer.allocate(size)
        codec.encode(value, buf)
        buf.flip()
        result = new Array[Byte](buf.remaining())
        buf.get(result)
      } catch {
        case _: java.nio.BufferOverflowException =>
          size *= 10
      }
    }
    if (result == null) {
      throw new Exception("Buffer overflow encoding object")
    }
    result
  }
}

case class Id(id: String) extends ByteConvert {
  def toBytes(): Array[Byte] = {
    BufferEncoder.encode(this, Id.codec)
  }
}
object Id {
  implicit val schema: Schema[Id] = Schema.derived
  implicit val codec: BinaryCodec[Id] = schema.derive(JsonFormat.deriver)
}

case class Image(image: Array[Byte]) extends ByteConvert { def toBytes(): Array[Byte] = { image } }

case class Answer(answer: String, id: String)
object Answer {
  implicit val schema: Schema[Answer] = Schema.derived
  implicit val codec: BinaryCodec[Answer] = schema.derive(JsonFormat.deriver)
}

case class Success(result: String) extends ByteConvert {
  def toBytes(): Array[Byte] = {
    BufferEncoder.encode(this, Success.codec)
  }
}
object Success {
  implicit val schema: Schema[Success] = Schema.derived
  implicit val codec: BinaryCodec[Success] = schema.derive(JsonFormat.deriver)
}

case class Error(message: String) extends ByteConvert {
  def toBytes(): Array[Byte] = {
    BufferEncoder.encode(this, Error.codec)
  }
}
object Error {
  implicit val schema: Schema[Error] = Schema.derived
  implicit val codec: BinaryCodec[Error] = schema.derive(JsonFormat.deriver)
}

case class CaptchaConfig(
    name: String,
    allowedLevels: List[String],
    allowedMedia: List[String],
    allowedInputType: List[String],
    allowedSizes: List[String],
    config: zio.blocks.schema.json.Json.Object
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
    authRequired: Option[Boolean] = None,
    captchas: List[CaptchaConfig] = List.empty
) {
  def toConfigField: ConfigField = ConfigField(
    port,
    address,
    bufferCount,
    seed,
    captchaExpiryTimeLimit,
    threadDelay,
    playgroundEnabled,
    corsHeader,
    maxAttemptsRatio,
    authRequired
  )
}
object AppConfig {
  implicit val schema: Schema[AppConfig] = Schema.derived
  implicit val codec: BinaryCodec[AppConfig] = schema.derive(JsonFormat.deriver)
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
    maxAttemptsRatio: Option[Float] = None,
    authRequired: Option[Boolean] = None
) {
  lazy val portInt: Option[Int] = port
  lazy val bufferCountInt: Option[Int] = bufferCount
  lazy val seedInt: Option[Int] = seed
  lazy val captchaExpiryTimeLimitInt: Option[Int] = captchaExpiryTimeLimit
  lazy val threadDelayInt: Option[Int] = threadDelay
  lazy val maxAttemptsRatioFloat: Option[Float] = maxAttemptsRatio
  lazy val playgroundEnabledBool: Option[Boolean] = playgroundEnabled.map(_ || false)
  lazy val authRequiredBool: Option[Boolean] = authRequired.map(_ || false)
}
