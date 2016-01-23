# scraper

Scrape content from Orange forums. Made with [scala-scraper](https://github.com/ruippeixotog/scala-scraper).

## Build Spark for Scala 2.11

Because it uses **Scala 2.11**, a version of Apache Spark supporting this version of Scala has to be built. To do this, first download Spark source code.

Then make `pom.xml` use Scala 2.11:

    ./dev/change-scala-version.sh 2.11
  
Build Spark with Maven:

    ./build/mvn -Pyarn -Phadoop-2.6 -Dhadoop.version=2.7.1 -Dscala-2.11 -DskipTests clean package
    
## Machine learning on sentiment analysis

Use `ml.ScrapeMessages` as:

    ./bin/spark-submit --class ml.ScrapeMessages scraper-assembly.jar
    
Messages are then stored in `$SPARK_DIR/ml/data` in partitioned files: `part-00000, part-00001, ...`. Merge them into a single text file to import it in Excel and save it as a `csv` file. This file alread exists in `ml` at the root of this directory.
    
## Run the scraper

Use `sbt assembly` to compile it and run: 

    ./bin/spark-submit --class Scraper scraper-assembly.jar
