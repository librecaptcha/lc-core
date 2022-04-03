package lc.core

import lc.captchas.interfaces.{Challenge, ChallengeProvider}
import lc.database.Statements
import java.io.ByteArrayInputStream
import java.sql.{Blob, ResultSet}
import java.util.UUID

class CaptchaManager(config: Config, captchaProviders: CaptchaProviders) {

  def getCaptcha(id: Id): Either[Error, Image] = {
    val blob = getImage(id.id)
    blob match {
      case Some(value) => {
        if (blob != null) {
          Right(Image(value.getBytes(1, value.length().toInt)))
        } else {
          Left(Error(ErrorMessageEnum.IMG_MISSING.toString))
        }
      }
      case None => Left(Error(ErrorMessageEnum.IMG_NOT_FOUND.toString))
    }
  }

  private def getImage(id: String): Option[Blob] = {
    val imagePstmt = Statements.tlStmts.get.imagePstmt
    imagePstmt.setString(1, id)
    val rs: ResultSet = imagePstmt.executeQuery()
    if (rs.next()) {
      Some(rs.getBlob("image"))
    } else {
      None
    }
  }

  def generateChallenge(param: Parameters): Option[Int] = {
    captchaProviders.getProvider(param).flatMap { provider =>
      val providerId = provider.getId()
      val challenge = provider.returnChallenge()
      val blob = new ByteArrayInputStream(challenge.content)
      val token = insertCaptcha(provider, challenge, providerId, param, blob)
      // println("Added new challenge: " + token.toString)
      token.map(_.toInt)
    }
  }

  private def insertCaptcha(
      provider: ChallengeProvider,
      challenge: Challenge,
      providerId: String,
      param: Parameters,
      blob: ByteArrayInputStream
  ): Option[Int] = {
    val insertPstmt = Statements.tlStmts.get.insertPstmt
    insertPstmt.setString(1, provider.getId)
    insertPstmt.setString(2, challenge.secret)
    insertPstmt.setString(3, providerId)
    insertPstmt.setString(4, challenge.contentType)
    insertPstmt.setString(5, param.level)
    insertPstmt.setString(6, param.input_type)
    insertPstmt.setBlob(7, blob)
    insertPstmt.executeUpdate()
    val rs: ResultSet = insertPstmt.getGeneratedKeys()
    if (rs.next()) {
      Some(rs.getInt("token"))
    } else {
      None
    }
  }

  val allowedInputType = config.allowedInputType
  val allowedLevels = config.allowedLevels
  val allowedMedia = config.allowedMedia

  private def validateParam(param: Parameters): Array[String] = {
    var invalid_params = Array[String]()
    if (!allowedLevels.contains(param.level)) invalid_params :+= "level"
    if (!allowedMedia.contains(param.media)) invalid_params :+= "media"
    if (!allowedInputType.contains(param.input_type)) invalid_params :+= "input_type"

    invalid_params
  }

  def getChallenge(param: Parameters): Either[Error, Id] = {
    val validParam = validateParam(param)
    if (validParam.isEmpty) {
      val tokenOpt = getToken(param)
      val token = tokenOpt.orElse(generateChallenge(param))
      token match {
        case Some(value) => {
          val uuid = getUUID(value)
          updateAttempted(value)
          Right(Id(uuid))
        }
        case None => {
          Left(Error(ErrorMessageEnum.NO_CAPTCHA.toString))
        }
      }
    } else {
      Left(Error(ErrorMessageEnum.INVALID_PARAM.toString + " => " + validParam.mkString(", ")))
    }
  }

  def getCount(param: Parameters): Option[Int] = {
    val countPstmt = Statements.tlStmts.get.countForParameterPstmt
    countPstmt.setString(1, param.level)
    countPstmt.setString(2, param.media)
    countPstmt.setString(3, param.input_type)
    val rs = countPstmt.executeQuery()
    if (rs.next()) {
      Some(rs.getInt("count"))
    } else {
      None
    }
  }

  private def getToken(param: Parameters): Option[Int] = {
    val count = getCount(param).getOrElse(0)
    if (count == 0) {
      None
    } else {
      val tokenPstmt = Statements.tlStmts.get.tokenPstmt
      tokenPstmt.setString(1, param.level)
      tokenPstmt.setString(2, param.media)
      tokenPstmt.setString(3, param.input_type)
      tokenPstmt.setInt(4, count)
      val rs = tokenPstmt.executeQuery()
      if (rs.next()) {
        Some(rs.getInt("token"))
      } else {
        None
      }
    }
  }

  private def updateAttempted(token: Int): Unit = {
    val updateAttemptedPstmt = Statements.tlStmts.get.updateAttemptedPstmt
    updateAttemptedPstmt.setInt(1, token)
    updateAttemptedPstmt.executeUpdate()
  }

  private def getUUID(id: Int): String = {
    val uuid = UUID.randomUUID().toString
    val mapPstmt = Statements.tlStmts.get.mapPstmt
    mapPstmt.setString(1, uuid)
    mapPstmt.setInt(2, id)
    mapPstmt.executeUpdate()
    uuid
  }

  def checkAnswer(answer: Answer): Either[Error, Success] = {
    val challenge = getSecret(answer.id)
    challenge match {
      case None => Right(Success(ResultEnum.EXPIRED.toString))
      case Some(value) => {
        val (provider, secret) = value
        val check = captchaProviders.getProviderById(provider).checkAnswer(secret, answer.answer)
        deleteCaptcha(answer.id)
        val result = if (check) ResultEnum.TRUE.toString else ResultEnum.FALSE.toString
        Right(Success(result))
      }
    }
  }

  private def getSecret(id: String): Option[(String, String)] = {
    val selectPstmt = Statements.tlStmts.get.selectPstmt
    selectPstmt.setInt(1, config.captchaExpiryTimeLimit)
    selectPstmt.setString(2, id)
    val rs: ResultSet = selectPstmt.executeQuery()
    if (rs.first()) {
      val secret = rs.getString("secret")
      val provider = rs.getString("provider")
      Some(provider, secret)
    } else {
      None
    }
  }

  private def deleteCaptcha(id: String): Unit = {
    val deleteAnswerPstmt = Statements.tlStmts.get.deleteAnswerPstmt
    deleteAnswerPstmt.setString(1, id)
    deleteAnswerPstmt.executeUpdate()
  }

  def display(): Unit = {
    val rs: ResultSet = Statements.tlStmts.get.getChallengeTable.executeQuery()
    println("token\t\tid\t\tsecret\t\tattempted")
    while (rs.next()) {
      val token = rs.getInt("token")
      val id = rs.getString("id")
      val secret = rs.getString("secret")
      val attempted = rs.getString("attempted")
      println(s"${token}\t\t${id}\t\t${secret}\t\t${attempted}\n\n")
    }

    val rss: ResultSet = Statements.tlStmts.get.getMapIdTable.executeQuery()
    println("uuid\t\ttoken\t\tlastServed")
    while (rss.next()) {
      val uuid = rss.getString("uuid")
      val token = rss.getInt("token")
      val lastServed = rss.getTimestamp("lastServed")
      println(s"${uuid}\t\t${token}\t\t${lastServed}\n\n")
    }
  }
}
