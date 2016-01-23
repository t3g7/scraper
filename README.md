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

Run `ml.Train` with the `csv` file to save a trained model:

    ./bin/spark-submit --class ml.Train scraper-assembly.jar <csv training file> <trained model save folder>

## Run the scraper

Use `sbt assembly` to compile it and run:

    ./bin/spark-submit --class Scraper scraper-assembly.jar
    
## Run the scraper on a Docker Swarm cluster

Deploy the Spark - Cassandra cluster with [t3g7/deployer](https://github.com/t3g7/deployer).

Then get Swarm nodes IPs with `docker-machine ip swarm-node-{1,2,3}` and replace `"Swarm node 1 IP,Swarm node 2 IP,Swarm node 3 IP"` with them as `spark.cassandra.connection.host` in `Scraper.scala`.

Copy the jar in `t3g7/deployer/data` and run it:

    docker exec -it swarm-master/master /usr/local/spark/bin/spark-submit --class Scraper --master spark://master:7077 /usr/local/spark/data/scraper-assembly-$VERSION.jar
