package lc.core

sealed trait ChallengeResult
case class Size(height: Int, width: Int)
case class Parameters(level: String, media: String, input_type: String, size: Option[Size])
case class Id(id: String) extends ChallengeResult
case class Image(image: Array[Byte]) extends ChallengeResult
case class Answer(answer: String, id: String)
case class Result(result: String) extends ChallengeResult
case class Error(message: String) extends ChallengeResult
case class Response(statusCode: Int, message: Array[Byte])
case class CaptchaConfig(
    name: String,
    allowedLevels: List[String],
    allowedMedia: List[String],
    allowedInputType: List[String],
    config: String
)
