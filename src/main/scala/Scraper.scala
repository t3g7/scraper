import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import org.jsoup.nodes.Element


object Scraper {
  def main(args: Array[String]): Unit = {
    val browser = new Browser

    /*
      url = http://communaute.orange.fr/
      path pour chaque sous-forum :

      --> Sujets sans réponse : http://communaute.orange.fr/t5/forums/unansweredtopicspage/page/1

      --> internet & fixe
          - les offres Internet Orange et options : /t5/les-offres-Internet-Orange-et/bd-p/ADSL
          - gérer mon offre Internet : /t5/gérer-mon-offre-Internet/bd-p/offre
          - homelive : /t5/homelive/bd-p/domotique
          - mon mail Orange : /t5/mon-mail-Orange/bd-p/mail
          - ma connexion : /t5/ma-connexion/bd-p/connexion
          - mon téléphone par internet et fixe : /t5/mon-téléphone-par-internet-et/bd-p/tel
          - protéger mes données et mon accès internet : /t5/protéger-mes-données-et-mon/bd-p/securite
          - mes services Orange : /t5/mes-services-Orange/bd-p/services

      --> TV d'Orange
          - TV par ADSL et Fibre : /t5/TV-par-ADSL-et-Fibre/bd-p/maTV
          - TV par Satellite : /t5/TV-par-Satellite/bd-p/TV
          - gerer mon offre TV : /t5/gerer-mon-offre-TV/bd-p/gerer
          - regarder la TV sur mon PC, ma tablette ou mon mobile : /t5/regarder-la-TV-sur-mon-PC-ma/bd-p/webtv
          - TV à la demande, OCS et VOD : /t5/TV-à-la-demande-OCS-et-VOD/bd-p/OCS

      --> mobile Orange
          - offres mobile Orange et options : /t5/offres-mobile-Orange-et-options/bd-p/offres
          - l'iPhone et ses applications : /t5/l-iPhone-et-ses-applications/bd-p/iphone-et-ses-applications
          - clé 3G Domino et Tablettes : /t5/clé-3G-Domino-et-Tablettes/bd-p/tablettes
          - utiliser mon mobile : /t5/utiliser-mon-mobile/bd-p/iphone
          - gérer mon offre mobile : /t5/gérer-mon-offre-mobile/bd-p/compte
     */


    val page = browser.get("http://communaute.orange.fr/t5/ma-connexion/bd-p/connexion")

    // Extract the elements with class "message-subject"
    val items: List[Element] = page >> elementList(".message-subject")

    // Extract the tr elements
    val threadTitles: List[String] = items.map(_ >> text("h3"))
    println(threadTitles)
  }
}