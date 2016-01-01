lazy val root = (project in file(".")).
  settings(
    name := "scraper",
    version := "0.1",
    scalaVersion := "2.11.7",
    mainClass in Compile := Some("Scraper")
  )

libraryDependencies ++= Seq(
  "com.github.scala-incubator.io" %% "scala-io-core" % "0.4.3",
  "com.github.scala-incubator.io" %% "scala-io-file" % "0.4.3",
  "org.apache.spark" %% "spark-core" % "1.5.1" % "provided",
  "org.apache.spark" %% "spark-streaming" % "1.5.1" % "provided",
  "com.datastax.spark" %% "spark-cassandra-connector" % "1.5.0-M2",
  "edu.stanford.nlp" % "stanford-corenlp" % "3.5.2",
  "net.ruippeixotog" % "scala-scraper_2.11" % "0.1.2"
)

// META-INF discarding
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}