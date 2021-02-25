package lc.database

import java.sql._

class DBConn() {
  val con: Connection = DriverManager.getConnection("jdbc:h2:./data/H2/captcha", "sa", "")

  def getStatement(): Statement = {
    con.createStatement()
  }

  def closeConnection(): Unit = {
    con.close()
  }
}
