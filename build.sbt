lazy val root = (project in file(".")).
  settings(
    name := "scraper",
    version := "0.1",
    scalaVersion := "2.11.7",
    mainClass in Compile := Some("Scraper")
  )

libraryDependencies += "net.ruippeixotog" % "scala-scraper_2.11" % "0.1.2"

// META-INF discarding
mergeStrategy in assembly <<= (mergeStrategy in assembly) { (old) =>
  {
    case PathList("META-INF", xs @ _*) => MergeStrategy.discard
    case x => MergeStrategy.first
  }
}