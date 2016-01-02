import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import org.jsoup.nodes.Element

object Scraper {
  def main(args: Array[String]): Unit = {
    val browser = new Browser
    val baseUrl = "http://communaute.orange.fr/t5"

    val subThreads = Seq(
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

    val page = browser.get("http://communaute.orange.fr/t5/ma-connexion/bd-p/connexion")

    // Extract the elements with class "message-subject"
    val items: List[Element] = page >> elementList(".message-subject")

    val topicTitles: List[String] = items.map(_ >> text("h3"))
    val topicLinks: List[String] = items.map(_ >> attr("href")("a"))

    println(topicTitles)
    println(topicLinks)
  }
}