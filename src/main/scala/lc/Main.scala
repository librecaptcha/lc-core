package lc

import com.sksamuel.scrimage._
import java.sql._
import java.io._

trait ChallengeProvider {
  val id: String
  def returnChallenge(): (Image, String)
  def checkAnswer(secret: String, answer: String): Boolean
  //TODO: def configure(): Unit
}

class Captcha {
  val con: Connection = DriverManager.getConnection("jdbc:h2:./captcha", "sa", "")
  val stmt: Statement = con.createStatement()
  stmt.execute("CREATE TABLE IF NOT EXISTS challenge(token varchar, id varchar, secret varchar, image blob)")
  val insertPstmt: PreparedStatement = con.prepareStatement("INSERT INTO challenge(token, id, secret) VALUES (?, ?, ?)")
  val selectPstmt: PreparedStatement = con.prepareStatement("SELECT secret FROM challenge WHERE token = ?")
  
  def getCaptcha(): Boolean = {
    val provider = new FilterChallenge
    val (token, image) = this.getChallenge(provider)
    image.output(new File("Captcha.png"))
    println(s"Token: ${token}")
    println("Enter your answer: ")
    val answer = scala.io.StdIn.readLine()
    this.getAnswer(token, answer, provider)
  }
  
  def getChallenge(provider: ChallengeProvider): (String, Image) = {
    val (image, secret) = provider.returnChallenge()
    val token = scala.util.Random.nextInt(10000).toString
    insertPstmt.setString(1, token)
    insertPstmt.setString(2, provider.id)
    insertPstmt.setString(3, secret)
    //TODO: insert image into database
    insertPstmt.executeUpdate()
    (token, image)
  }

  def getAnswer(token: String, answer: String, provider: ChallengeProvider): Boolean = {
    selectPstmt.setString(1, token)
    val rs: ResultSet = selectPstmt.executeQuery()
    rs.next()
    val secret = rs.getString("secret")
    provider.checkAnswer(secret, answer)
  }

  def display(): Unit = {
    val rs: ResultSet = stmt.executeQuery("SELECT * FROM challenge")
    println("token\t\tid\t\tsecret\t\timage")
    while(rs.next()) {
      val token = rs.getString("token")
      val id = rs.getString("id")
      val secret = rs.getString("secret")
      val image = rs.getString("image")
      println(s"${token}\t\t${id}\t\t${secret}\t\t${image}")
    }
  }
  
  def closeConnection(): Unit = {
	  con.close()
  } 
}

object LCFramework{
  def main(args: scala.Array[String]) {
    val captcha = new Captcha
    val result = captcha.getCaptcha()
    println(result)
    captcha.display()
    captcha.closeConnection()
  } 
}

