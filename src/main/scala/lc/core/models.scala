package lc.core

import org.json4s.jackson.Serialization.write
import lc.core.Config.formats

trait ByteConvert { def toBytes(): Array[Byte] }
case class Size(height: Int, width: Int)
case class Parameters(level: String, media: String, input_type: String, size: Option[Size])
case class Id(id: String) extends ByteConvert { def toBytes(): Array[Byte] = { write(this).getBytes } }
case class Image(image: Array[Byte]) extends ByteConvert { def toBytes(): Array[Byte] = { image } }
case class Answer(answer: String, id: String)
case class Success(result: String) extends ByteConvert { def toBytes(): Array[Byte] = { write(this).getBytes } }
case class Error(message: String) extends ByteConvert { def toBytes(): Array[Byte] = { write(this).getBytes } }
case class Response(statusCode: Int, message: Array[Byte])
case class CaptchaConfig(
    name: String,
    allowedLevels: List[String],
    allowedMedia: List[String],
    allowedInputType: List[String],
    config: String
)
