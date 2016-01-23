package ml

import org.apache.spark.{SparkContext, SparkConf}
import org.apache.spark.mllib.tree.model.RandomForestModel

object Predict {
  def predictSentiment(trainedModelDir: String, text: String): String = {

    val conf = new SparkConf()
      .setMaster("local[2]")
      .setAppName("Orange Forums Sentiment Prediction")
    val sc = new SparkContext(conf)

    val model = RandomForestModel.load(sc, trainedModelDir.toString)
    val labeledStatuses = model.predict(Train.featurize(text))

    labeledStatuses.toString
  }
}