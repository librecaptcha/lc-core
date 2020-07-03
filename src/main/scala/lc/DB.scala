package lc

import java.sql._

class DBConn(){
  val con: Connection = DriverManager.getConnection("jdbc:h2:./captcha", "sa", "")

  lazy val insertPstmt: PreparedStatement = con.prepareStatement("INSERT INTO challenge(token, id, secret, provider, contentType, image) VALUES (?, ?, ?, ?, ?, ?)")
  lazy val mapPstmt: PreparedStatement = con.prepareStatement("INSERT INTO mapId(uuid, token) VALUES (?, ?)")
  lazy val selectPstmt: PreparedStatement = con.prepareStatement("SELECT secret, provider FROM challenge WHERE token = ?")
  lazy val imagePstmt: PreparedStatement = con.prepareStatement("SELECT image FROM challenge c, mapId m WHERE c.token=m.token AND m.uuid = ?")
  lazy val updatePstmt: PreparedStatement = con.prepareStatement("UPDATE challenge SET solved = True WHERE token = (SELECT m.token FROM mapId m, challenge c WHERE m.token=c.token AND m.uuid = ?)")
  lazy val userPstmt: PreparedStatement = con.prepareStatement("INSERT INTO users(email, hash) VALUES (?,?)")
  lazy val validatePstmt: PreparedStatement = con.prepareStatement("SELECT hash FROM users WHERE hash = ? LIMIT 1")

  def getConn(): Statement = {
    con.createStatement()
  }

  def closeConnection(): Unit = {
	  con.close()
  }
}
