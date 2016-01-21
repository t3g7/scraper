package utils

import com.datastax.spark.connector.cql.CassandraConnector
import org.apache.spark.SparkConf

object CassandraSettings {

  /**
    * Set up the keyspace and tables used for storing messages
    * @param conf
    */
  def setUp(conf: SparkConf): Unit = {
    CassandraConnector(conf).withSessionDo { session =>
      session.execute("CREATE KEYSPACE IF NOT EXISTS forums WITH REPLICATION = {'class': 'SimpleStrategy', 'replication_factor': 3}")

      session.execute(
        """
        CREATE TABLE IF NOT EXISTS forums.threads (
          title text,
          link text,
          date timestamp,
          messages frozen <list<map<text, text>>>,
          PRIMARY KEY (title)
        )"""
      )
    }
  }
}