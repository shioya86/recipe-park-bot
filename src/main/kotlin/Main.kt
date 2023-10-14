import org.jsoup.*
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.DayOfWeek
import java.time.LocalDateTime

fun main() {
  // 味の素パークから今日の献立を取得
  val document = Jsoup.connect("https://park.ajinomoto.co.jp/menu/").get()
  val elements = document.select(".weeklyMenuCarouselCard")

  val todayMenuCard = elements[getDayOfWeekIndex()]
  val menuUrl = todayMenuCard.select("a").attr("href")

  //// 今日の日付と情報を取得
  val todayMenuHeader = todayMenuCard.select(".cardHeader")
  val dateStr = todayMenuHeader.select(".date").text()

  //// 御膳の画像を取得
  val todayMenuBody = todayMenuCard.select(".cardBody")
  val todayMenuImages = todayMenuBody.select(".imageWrapper img").map {
    it.attr("src")
  }

  //// 御膳の構成と料理名の取得
  val todayMenuTypes = todayMenuBody.select(".titleWrapper .recipeGroup").eachText()
  val todayMenuNames = todayMenuBody.select(".titleWrapper .recipeTitle").eachText()

  //// 送信用メッセージの生成
  val bodyData = generateSendJsonData(dateStr, menuUrl, todayMenuTypes, todayMenuNames, todayMenuImages).toByteArray()

  //// Discordにメッセージを送信
  val webhookURL = System.getenv("DISCORD_WEBHOOK_URL")
  if (webhookURL == null) {
    System.err.println("Error: Cannot read environment variable 'DISCORD_WEBHOOK_URL'")
  }
  postDiscord(webhookURL, bodyData)
}

fun getDayOfWeekIndex (): Int = when (LocalDateTime.now().dayOfWeek) {
  DayOfWeek.SUNDAY -> 0
  DayOfWeek.MONDAY -> 1
  DayOfWeek.TUESDAY -> 2
  DayOfWeek.WEDNESDAY -> 3
  DayOfWeek.THURSDAY -> 4
  DayOfWeek.FRIDAY -> 5
  DayOfWeek.SATURDAY -> 6
  else -> 0
}

fun generateSendJsonData (todayTitle: String, menuUrl: String, todayMenuTypes: List<String>, todayMenuNames: List<String>, todayMenuImages: List<String>): String =
"""
  {
    "username": "【非公式】味の素パーク 今日の献立",
    "content": "${todayTitle}の献立\n:point_right: $menuUrl",
    "embeds": [
      {
        "fields": [
          ${(0..2).joinToString { """{ "name": "${todayMenuTypes[it]}", "value": "${todayMenuNames[it]}" }""" }}
        ]
      },
      ${(0..2).joinToString { """{ "url": "$menuUrl", "image": { "url": "${todayMenuImages[it]}" } }""" }}
    ]
  }
"""

fun postDiscord(webhookURL: String, bodyData: ByteArray): Result<Int> {
  val url = URL(webhookURL)
  val con = url.openConnection() as HttpURLConnection

  con.requestMethod = "POST"
  con.doOutput = true
  con.setRequestProperty("Content-type", "application/json; charset=utf-8")
  con.setRequestProperty("User-Agent", "")

  con.setChunkedStreamingMode(0)

  con.connect()

  val outputStream = con.outputStream
  outputStream.write(bodyData)
  outputStream.flush()
  outputStream.close()

  val responseCode = con.responseCode
  if (responseCode !in 200..299) {
    // エラー処理
    System.err.println("Error: Response code is ${con.responseCode}")
    return Result.failure(IOException())
  }

  con.disconnect()
  return Result.success(con.responseCode)
}

