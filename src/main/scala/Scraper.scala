import ml.Predict._

import java.text.SimpleDateFormat
import java.util.Calendar

import com.datastax.spark.connector._
import org.apache.spark.{SparkContext, SparkConf}
import utils.CassandraSettings

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

    val timestampFormatBySecond = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    val timestampFormatByMinute = new SimpleDateFormat("yyyy-MM-dd HH:mm")

    var countPass = 0
    while (true) {
      val browser = new Browser
      val baseUrl = "http://communaute.orange.fr"

      val subForums = Seq(
        "/forums/unansweredtopicspage", // Sujets sans réponse

        // internet & fixe :
        "/les-offres-Internet-Orange-et/bd-p/ADSL", // les offres Internet Orange et options
        "/gérer-mon-offre-Internet/bd-p/offre", // gérer mon offre Internet
        "/homelive/bd-p/domotique", // homelive
        "/mon-mail-Orange/bd-p/mail", // mon mail Orange
        "/ma-connexion/bd-p/connexion", // ma connexion
        "/mon-téléphone-par-internet-et/bd-p/tel", // mon téléphone par internet et fixe
        "/protéger-mes-données-et-mon/bd-p/securite", // protéger mes données et mon accès internet
        "/mes-services-Orange/bd-p/services", // mes services Orange

        // TV d'Orange
        "/TV-par-ADSL-et-Fibre/bd-p/maTV", // TV par ADSL et Fibre
        "/TV-par-Satellite/bd-p/TV", // TV par Satellite
        "/gerer-mon-offre-TV/bd-p/gerer", // gerer mon offre TV
        "/regarder-la-TV-sur-mon-PC-ma/bd-p/webtv", // regarder la TV sur mon PC
        "/TV-à-la-demande-OCS-et-VOD/bd-p/OCS", // TV à la demande, OCS et VOD

        // mobile Orange
        "/offres-mobile-Orange-et-options/bd-p/offres", // offres mobile Orange et options
        "/l-iPhone-et-ses-applications/bd-p/iphone-et-ses-applications", // l'iPhone et ses applications
        "/clé-3G-Domino-et-Tablettes/bd-p/tablettes", // clé 3G Domino et Tablettes
        "/utiliser-mon-mobile/bd-p/iphone", // utiliser mon mobile
        "/gérer-mon-offre-mobile/bd-p/compte" // gérer mon offre mobile
      )

      val subForumPage = browser.get("http://communaute.orange.fr/t5/ma-connexion/bd-p/connexion")

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
      val threadDateTimes = (threadDates zip threadTimes) map { case (d, t) => convertDate(d) + " " + convertTime(t) }
      val threadTimestamp = threadDateTimes.map(x => timestampFormatByMinute.format(timestampFormatByMinute.parse(x)))

      val now = timestampFormatByMinute.format(Calendar.getInstance().getTime())
      // To do: return a list of indexes for threads with same date
      val indexNewThread = threadTimestamp.indexOf(now)

      if (countPass == 0) {

        // Extract messages of threads
        var threadMessages = new ListBuffer[List[Map[String, String]]]()
        for (thread <- threadLinks) {
          val threadPage = browser.get(thread)
          val threadMessagesItems: List[Element] = threadPage >> elementList(".lia-message-body-content")

          var messages = new ListBuffer[Map[String, String]]()
          for (threadMessage <- threadMessagesItems) {
            val message = threadMessage >> extractor(".lia-message-body-content", text)
            //messages += Map(message -> predictSentiment(trainedModelDir, message))
            messages += Map(message -> "SENTIMENT")
          }
          threadMessages += messages.toList
        }

        // Combine title, link, timestamp and messages of each thread
        val threadMessagesList = threadMessages.toList
        val threads = ((threadTitles zip threadLinks) zip threadDateTimes) zip threadMessagesList map {
          case (((threadTitles, threadLinks), threadDateTimes), threadMessagesList) =>
            (threadTitles, threadLinks, threadDateTimes, threadMessagesList)
        }

        println("Saving " + threads.length + " threads - " + timestampFormatByMinute.format(Calendar.getInstance().getTime()))

        val threadsRDD = sc.makeRDD(threads.toSeq)
        threadsRDD.saveToCassandra("forums", "threads", SomeColumns("title", "link", "date", "messages"))

        countPass += 1
      } else {
        if (indexNewThread != -1) {

          // Extract messages of the updated or new thread
          var threadMessages = new ListBuffer[List[Map[String, String]]]()
          val threadPage = browser.get(threadLinks(indexNewThread))
          val threadMessagesItems: List[Element] = threadPage >> elementList(".lia-message-body-content")

          var messages = new ListBuffer[Map[String, String]]()
          for (threadMessage <- threadMessagesItems) {
            val message = threadMessage >> extractor(".lia-message-body-content", text)
            //messages += Map(message -> predictSentiment(trainedModelDir, message))
            messages += Map(message -> "SENTIMENT")
          }
          threadMessages += messages.toList

          // Combine title, link, timestamp and messages of each thread
          val threadMessagesList = threadMessages.toList
          val threads = ((threadTitles zip threadLinks) zip threadDateTimes) zip threadMessagesList map {
            case (((threadTitles, threadLinks), threadDateTimes), threadMessagesList) =>
              (threadTitles, threadLinks, threadDateTimes, threadMessagesList)
          }

          val newThread = List(threads(0)).toSeq
          println("Saving new thread : " + newThread(0) + " - " + timestampFormatBySecond.format(Calendar.getInstance().getTime()))

          val threadsRDD = sc.makeRDD(newThread)
          threadsRDD.saveToCassandra("forums", "threads", SomeColumns("title", "link", "date", "messages"))
        } else {
          println("No new threads - " + timestampFormatBySecond.format(Calendar.getInstance().getTime()))
        }

        countPass += 1
      }

      Thread.sleep(15000)
    }
  }

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