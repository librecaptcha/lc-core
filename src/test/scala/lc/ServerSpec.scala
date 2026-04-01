package lc.server

import java.net.{HttpURLConnection, URL}
import java.io.{BufferedReader, InputStreamReader, OutputStreamWriter}
import lc.LCFramework

object ServerSpec {
  def main(args: Array[String]): Unit = {
    // Start server before tests in a thread
    val serverRunnable = new Runnable {
      override def run(): Unit = {
        try {
          LCFramework.main(Array.empty)
        } catch {
          case _: InterruptedException => // Expected on shutdown
        }
      }
    }
    val serverThread = new Thread(serverRunnable)
    serverThread.start()

    // Give the server a few seconds to start
    Thread.sleep(5000)

    try {
      println("Running ServerSpec Test...")
      val url = new URL("http://localhost:8888/v2/captcha")
      val connection = url.openConnection().asInstanceOf[HttpURLConnection]
      connection.setRequestMethod("POST")
      connection.setRequestProperty("Content-Type", "application/json")
      connection.setDoOutput(true)

      val payload = """{"level":"easy","media":"image/png","input_type":"text","size":"350x100"}"""
      val out = new OutputStreamWriter(connection.getOutputStream)
      out.write(payload)
      out.close()

      val responseCode = connection.getResponseCode
      assert(responseCode == 200, s"Expected 200 but got $responseCode")

      val in = new BufferedReader(new InputStreamReader(connection.getInputStream))
      val response = new StringBuilder
      var line: String = in.readLine()
      while (line != null) {
        response.append(line)
        line = in.readLine()
      }
      in.close()

      val responseString = response.toString()
      assert(responseString.contains("id"), "Response did not contain an id")
      println("Test Passed.")
    } finally {
      // Shutdown server without exit so SBT doesn't kill the VM
      System.exit(0)
    }
  }
}
