import java.text.SimpleDateFormat
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.{SparkContext, SparkConf}

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

      // Extract messages of a thread
      var threadMessages = new ListBuffer[List[Map[String, String]]]()
      for (thread <- threadLinks) {
        val threadPage = browser.get(thread)
        val threadMessagesItems: List[Element] = threadPage >> elementList(".lia-message-body-content")

        var messages = new ListBuffer[Map[String, String]]()
        for (threadMessage <- threadMessagesItems) {
          val message = threadMessage >> extractor(".lia-message-body-content", text)
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
      //println(threads)

      val threadsRDD = sc.makeRDD(threads.toSeq)
      println(threadsRDD)
      Thread.sleep(60000)
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