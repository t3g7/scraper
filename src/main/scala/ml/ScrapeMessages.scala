package ml

import java.io.File

import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors._
import org.apache.hadoop.fs.FileUtil
import org.apache.spark.{SparkConf, SparkContext}
import org.jsoup.nodes.Element

import scala.collection.mutable.ListBuffer

/**
 * Scrape messages from Orange forums and write
 * them to a CSV file for further classification
 */
object ScrapeMessages {

  def main(args: Array[String]) {
    val conf = new SparkConf().setAppName("ScrapeAndWriteToCSV")
    val sc = new SparkContext(conf)

    val browser = new Browser
    val baseUrl = "http://communaute.orange.fr"

    val subForums = Seq(
      "/t5/forums/unansweredtopicspage", // Sujets sans réponse

      // internet & fixe :
      "/t5/les-offres-Internet-Orange-et/bd-p/ADSL", // les offres Internet Orange et options
      "/t5/gérer-mon-offre-Internet/bd-p/offre", // gérer mon offre Internet
      "/t5/homelive/bd-p/domotique", // homelive
      "/t5/mon-mail-Orange/bd-p/mail", // mon mail Orange
      "/t5/ma-connexion/bd-p/connexion", // ma connexion
      "/t5/mon-téléphone-par-internet-et/bd-p/tel", // mon téléphone par internet et fixe
      "/t5/protéger-mes-données-et-mon/bd-p/securite", // protéger mes données et mon accès internet
      "/t5/mes-services-Orange/bd-p/services", // mes services Orange

      // TV d'Orange
      "/t5/TV-par-ADSL-et-Fibre/bd-p/maTV", // TV par ADSL et Fibre
      "/t5/TV-par-Satellite/bd-p/TV", // TV par Satellite
      "/t5/gerer-mon-offre-TV/bd-p/gerer", // gerer mon offre TV
      "/t5/regarder-la-TV-sur-mon-PC-ma/bd-p/webtv", // regarder la TV sur mon PC
      "/t5/TV-à-la-demande-OCS-et-VOD/bd-p/OCS", // TV à la demande, OCS et VOD

      // mobile Orange
      "/t5/offres-mobile-Orange-et-options/bd-p/offres", // offres mobile Orange et options
      "/t5/l-iPhone-et-ses-applications/bd-p/iphone-et-ses-applications", // l'iPhone et ses applications
      "/t5/clé-3G-Domino-et-Tablettes/bd-p/tablettes", // clé 3G Domino et Tablettes
      "/t5/utiliser-mon-mobile/bd-p/iphone", // utiliser mon mobile
      "/t5/gérer-mon-offre-mobile/bd-p/compte" // gérer mon offre mobile
    )

    var messages = new ListBuffer[String]()

    for (subForumLink <- subForums) {
      val subForumPage = browser.get(baseUrl + subForumLink)

      // Extract the thread rows
      val threadTitlesItems: List[Element] = subForumPage >> elementList(".lia-list-row-thread-unread")
      val threadRelLinks: List[String] = threadTitlesItems.map(_ >> attr("href")("a"))
      val threadLinks: List[String] = threadRelLinks.map { l => baseUrl + l }

      // Extract messages of threads

      for (thread <- threadLinks) {
        val threadPage = browser.get(thread)
        val threadMessagesItems: List[Element] = threadPage >> elementList(".lia-message-body-content")

        for (threadMessage <- threadMessagesItems) {
          val message = threadMessage >> extractor(".lia-message-body-content", text)
          messages += message
        }
      }
    }

    println("Saved " + messages.length + " messages")

    val trainingDatasetFile = "data/ml/training-data.csv"
    FileUtil.fullyDelete(new File(trainingDatasetFile))

    val messageRDD = sc.parallelize(messages.toSeq)
    messageRDD.saveAsTextFile(trainingDatasetFile)
  }
}
