package utils

import java.text.SimpleDateFormat

object Time {

  val timestampFormatBySecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
  val timestampFormatByMinute = new SimpleDateFormat("yyyy-MM-dd HH:mm")

  def convertDate(date: String): String = {
    val format = new SimpleDateFormat("dd-MM-yyyy")
    val parsedDate = format.parse(date)
    new SimpleDateFormat("yyyy-MM-dd").format(parsedDate)
  }

  def convertTime(time: String): String = {
    val format = new SimpleDateFormat("HH'h'mm")
    val parsedTime = format.parse(time)
    new SimpleDateFormat("HH:mm").format(parsedTime)
  }
}
