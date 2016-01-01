import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.dsl.DSL.Extract._
import org.jsoup.nodes.Element


object Scraper {
  def main(args: Array[String]): Unit = {
    val browser = new Browser
    val page = browser.get("http://communaute.orange.fr/t5/ma-connexion/bd-p/connexion")

    // Extract the elements with class "message-subject"
    val items: List[Element] = page >> elementList(".message-subject")

    // Extract the tr elements
    val threadTitles: List[String] = items.map(_ >> text("h3"))
    println(threadTitles)
  }
}