package lc.database

import java.sql.{Connection, DriverManager, Statement}

class DBConn() {
  val con: Connection = DriverManager.getConnection("jdbc:h2:./data/H2/captcha2", "sa", "")

  def getStatement(): Statement = {
    con.createStatement()
  }

  def closeConnection(): Unit = {
    con.close()
  }
}
