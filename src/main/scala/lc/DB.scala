package lc

import java.sql._

class DBConn(){
  val con: Connection = DriverManager.getConnection("jdbc:h2:./captcha", "sa", "")

  def getStatement(): Statement = {
    con.createStatement()
  }

  def closeConnection(): Unit = {
	  con.close()
  }
}
