import ml.Predict._

import java.util.Calendar

import com.datastax.spark.connector._
import org.apache.spark.{SparkContext, SparkConf}
import utils.{Time, CassandraSettings}

import scala.collection.mutable.ListBuffer

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import org.jsoup.nodes.Element

object Scraper {

  def main(args: Array[String]): Unit = {

    val conf = new SparkConf()
      .setMaster("local[*]")
      .setAppName("OFScraper")
      .set("spark.cassandra.connection.host", "localhost")
    val sc = new SparkContext(conf)

    //val trainedModelDir = args(0)
    CassandraSettings.setUp(conf)

    val timestampFormatBySecond = Time.timestampFormatBySecond
    val timestampFormatByMinute = Time.timestampFormatByMinute

    var countPass = 0
    while (true) {
      val browser = new Browser
      val baseUrl = Links.baseUrl
      val subForums = Links.subForums

      for (subForumLink <- subForums) {
        val subForumPage = browser.get(baseUrl + subForumLink)

        // Extract the thread rows
        val threadTitlesItems: List[Element] = subForumPage >> elementList(".lia-list-row-thread-unread")
        val threadTitles: List[String] = threadTitlesItems.map(_ >> text("h3"))
        val threadRelLinks: List[String] = threadTitlesItems.map(_ >> attr("href")("a"))
        val threadLinks: List[String] = threadRelLinks.map { l => baseUrl + l }

        val threadDateTimeItems: List[Element] = subForumPage >> elementList(".DateTime")
        var threadDates: List[String] = threadDateTimeItems.map(_ >> text(".local-date"))
        val threadTimes: List[String] = threadDateTimeItems.map(_ >> text(".local-time"))

        // Remove &lrm; character in HTML
        threadDates = threadDates.map(d => d.replace("\u200E", ""))
        val threadDateTimes = (threadDates zip threadTimes) map { case (d, t) => Time.convertDate(d) + " " + Time.convertTime(t) }
        val threadTimestamp = threadDateTimes.map(x => timestampFormatByMinute.format(timestampFormatByMinute.parse(x)))

        val now = timestampFormatByMinute.format(Calendar.getInstance().getTime())
        // To do: return a list of indexes for threads with same date
        val indexNewThread = threadTimestamp.indexOf(now)

        // Scrape all messages from the subForums sequence
        if (countPass < subForums.length) {

          // Extract messages of threads
          var threadMessages = new ListBuffer[List[Map[String, String]]]()
          for (thread <- threadLinks) {
            MessagesExtractor.extract(browser, thread, threadMessages)
          }
          val threadMessagesList = threadMessages.toList

          // Combine title, link, timestamp and messages of each thread
          val threads = ((threadTitles zip threadLinks) zip threadDateTimes) zip threadMessagesList map {
            case (((threadTitles, threadLinks), threadDateTimes), threadMessagesList) =>
              (threadTitles, threadLinks, threadDateTimes, threadMessagesList)
          }

          println("Saving " + threads.length + " threads from subforum " + subForumLink.split("/").last + " - " + timestampFormatBySecond.format(Calendar.getInstance().getTime()))

          val threadsRDD = sc.makeRDD(threads.toSeq)
          threadsRDD.saveToCassandra("forums", "threads", SomeColumns("title", "link", "date", "messages"))

          countPass += 1
        } else {
          if (indexNewThread != -1) {

            // Extract messages of the updated or new thread
            var threadMessages = new ListBuffer[List[Map[String, String]]]()
            MessagesExtractor.extract(browser, threadLinks(indexNewThread), threadMessages)
            val threadMessagesList = threadMessages.toList

            // Combine title, link, timestamp and messages of each thread
            val threads = ((threadTitles zip threadLinks) zip threadDateTimes) zip threadMessagesList map {
              case (((threadTitles, threadLinks), threadDateTimes), threadMessagesList) =>
                (threadTitles, threadLinks, threadDateTimes, threadMessagesList)
            }

            val newThread = List(threads(0)).toSeq
            println("Saving new thread : " + newThread(0) + " " + subForumLink.split("/").last + " - " + timestampFormatBySecond.format(Calendar.getInstance().getTime()))

            val threadsRDD = sc.makeRDD(newThread)
            threadsRDD.saveToCassandra("forums", "threads", SomeColumns("title", "link", "date", "messages"))
          } else {
            println("Sub forum: " + subForumLink.split("/").last + " - No new threads - " + timestampFormatBySecond.format(Calendar.getInstance().getTime()))
          }

          countPass += 1
          Thread.sleep(15000)
        }
      }
    }
  }
}