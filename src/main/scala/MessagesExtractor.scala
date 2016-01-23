import net.ruippeixotog.scalascraper.browser.Browser
import net.ruippeixotog.scalascraper.dsl.DSL._
import net.ruippeixotog.scalascraper.scraper.ContentExtractors._
import org.jsoup.nodes.Element

import scala.collection.mutable.ListBuffer

object MessagesExtractor {

  /**
    * Extract messages of given thread
    * @param browser
    * @param threadLink
    * @param threadMessages
    * @return
    */
  def extract(browser: Browser, threadLink: String, threadMessages: ListBuffer[List[Map[String, String]]]) {
    val threadPage = browser.get(threadLink)
    val threadMessagesItems: List[Element] = threadPage >> elementList(".lia-message-body-content")

    var messages = new ListBuffer[Map[String, String]]()
    for (threadMessage <- threadMessagesItems) {
      val message = threadMessage >> extractor(".lia-message-body-content", text)
      //messages += Map(message -> predictSentiment(trainedModelDir, message))
      messages += Map(message -> "SENTIMENT")
    }
    threadMessages += messages.toList
  }
}
