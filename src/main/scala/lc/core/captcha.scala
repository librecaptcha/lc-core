package lc.core

import java.sql.ResultSet
import java.util.UUID
import java.io.ByteArrayInputStream
import lc.database.Statements
import lc.core.CaptchaProviders
import java.sql.Blob


object Captcha {

  def getCaptcha(id: Id): Array[Byte] = {
    var image: Array[Byte] = null
    var blob: Blob = null
    try {
      val imagePstmt = Statements.tlStmts.get.imagePstmt
      imagePstmt.setString(1, id.id)
      val rs: ResultSet = imagePstmt.executeQuery()
      if (rs.next()) {
        blob = rs.getBlob("image")
        if (blob != null) {
          image = blob.getBytes(1, blob.length().toInt)
        }
      }
      image
    } catch {
      case e: Exception =>
        println(e)
        image
    }
  }

  def generateChallenge(param: Parameters): Int = {
    val provider = CaptchaProviders.getProvider(param)
    val providerId = provider.getId()
    val challenge = provider.returnChallenge()
    val blob = new ByteArrayInputStream(challenge.content)
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
    val token = if (rs.next()) {
      rs.getInt("token")
    }
    println("Added new challenge: " + token.toString)
    token.asInstanceOf[Int]
  }

  val allowedInputType = Config.allowedInputType
  val allowedLevels = Config.allowedLevels
  val allowedMedia = Config.allowedMedia

  private def validateParam(param: Parameters): Boolean = {
    if (
      allowedLevels.contains(param.level) &&
      allowedMedia.contains(param.media) &&
      allowedInputType.contains(param.input_type)
    )
      return true
    else
      return false
  }

   def getChallenge(param: Parameters): ChallengeResult = {
    try {
      val validParam = validateParam(param)
      if (validParam) {
        val tokenPstmt = Statements.tlStmts.get.tokenPstmt
        tokenPstmt.setString(1, param.level)
        tokenPstmt.setString(2, param.media)
        tokenPstmt.setString(3, param.input_type)
        val rs = tokenPstmt.executeQuery()
        val tokenOpt = if (rs.next()) {
          Some(rs.getInt("token"))
        } else {
          None
        }
        val updateAttemptedPstmt = Statements.tlStmts.get.updateAttemptedPstmt
        val token = tokenOpt.getOrElse(generateChallenge(param))
        val result = if (token != -1) {
          val uuid = getUUID(token)
          updateAttemptedPstmt.setString(1, uuid)
          updateAttemptedPstmt.executeUpdate()
          Id(uuid)
        } else {
          Error(ErrorMessageEnum.NO_CAPTCHA.toString)
        }
        result
      } else {
        Error(ErrorMessageEnum.INVALID_PARAM.toString)
      }
    } catch {
      case e: Exception =>
        println(e)
        Error(ErrorMessageEnum.SMW.toString)
    }
  }

  private def getUUID(id: Int): String = {
    val uuid = UUID.randomUUID().toString
    val mapPstmt = Statements.tlStmts.get.mapPstmt
    mapPstmt.setString(1, uuid)
    mapPstmt.setInt(2, id)
    mapPstmt.executeUpdate()
    uuid
  }

  def checkAnswer(answer: Answer): Result = {
    val selectPstmt = Statements.tlStmts.get.selectPstmt
    selectPstmt.setInt(1, Config.captchaExpiryTimeLimit)
    selectPstmt.setString(2, answer.id)
    val rs: ResultSet = selectPstmt.executeQuery()
    val psOpt = if (rs.first()) {
      val secret = rs.getString("secret")
      val provider = rs.getString("provider")
      val check = CaptchaProviders.getProviderById(provider).checkAnswer(secret, answer.answer)
      val result = if (check) ResultEnum.TRUE.toString else ResultEnum.FALSE.toString
      result
    } else {
      ResultEnum.EXPIRED.toString
    }
    val deleteAnswerPstmt = Statements.tlStmts.get.deleteAnswerPstmt
    deleteAnswerPstmt.setString(1, answer.id)
    deleteAnswerPstmt.executeUpdate()
    Result(psOpt)
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
