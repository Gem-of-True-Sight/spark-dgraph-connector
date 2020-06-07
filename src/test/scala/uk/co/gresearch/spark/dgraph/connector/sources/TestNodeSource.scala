package uk.co.gresearch.spark.dgraph.connector.sources

import org.apache.spark.sql.execution.datasources.v2.DataSourceRDDPartition
import org.scalatest.FunSpec
import uk.co.gresearch.spark.SparkTestSession
import uk.co.gresearch.spark.dgraph.connector._

class TestNodeSource extends FunSpec with SparkTestSession {

  import spark.implicits._

  describe("NodeDataSource") {

    it("should load nodes via path") {
      spark
        .read
        .format(NodesSource)
        .load("localhost:9080")
        .show(100, false)
    }

    it("should load nodes via paths") {
      spark
        .read
        .format(NodesSource)
        .load("localhost:9080", "127.0.0.1:9080")
        .show(100, false)
    }

    it("should load nodes via target option") {
      spark
        .read
        .format(NodesSource)
        .option(TargetOption, "localhost:9080")
        .load()
        .show(100, false)
    }

    it("should load nodes via targets option") {
      spark
        .read
        .format(NodesSource)
        .option(TargetsOption, "[\"localhost:9080\",\"127.0.0.1:9080\"]")
        .load()
        .show(100, false)
    }

    it("should load nodes via implicit dgraph target") {
      spark
        .read
        .dgraphNodes("localhost:9080")
        .show(100, false)
    }

    it("should load nodes via implicit dgraph targets") {
      spark
        .read
        .dgraphNodes("localhost:9080", "127.0.0.1:9080")
        .show(100, false)
    }

    it("should encode TypedNode") {
      val rows =
        spark
          .read
          .format(NodesSource)
          .load("localhost:9080")
          .as[TypedNode]
          .collectAsList()
      rows.forEach(println)
    }

    it("should fail without target") {
      assertThrows[IllegalArgumentException] {
        spark
          .read
          .format(NodesSource)
          .load()
      }
    }

    it("should load as a single partition") {
      val target = "localhost:9080"
      val targets = Seq(Target(target))
      val partitions =
        spark
          .read
          .option(PartitionerOption, SingletonPartitionerOption)
          .dgraphNodes(target)
          .rdd
          .partitions.map {
          case p: DataSourceRDDPartition => Some(p.inputPartition)
          case _ => None
        }
      assert(partitions.length === 1)
      assert(partitions === Seq(Some(Partition(targets, None, None))))
    }

    it("should load as a predicate partitions") {
      val target = "localhost:9080"
      val partitions =
        spark
          .read
          .option(PartitionerOption, PredicatePartitionerOption)
          .option(PredicatePartitionerPredicatesOption, "2")
          .dgraphNodes(target)
          .rdd
          .partitions.map {
          case p: DataSourceRDDPartition => Some(p.inputPartition)
          case _ => None
        }
      assert(partitions.length === 4)
      assert(partitions === Seq(
        Some(Partition(Seq(Target("localhost:9080")), Some(Set(Predicate("release_date", "datetime"), Predicate("running_time", "int"))), None)),
        Some(Partition(Seq(Target("localhost:9080")), Some(Set(Predicate("dgraph.graphql.schema", "string"), Predicate("name", "string"))), None)),
        Some(Partition(Seq(Target("localhost:9080")), Some(Set(Predicate("dgraph.type", "string"))), None)),
        Some(Partition(Seq(Target("localhost:9080")), Some(Set(Predicate("revenue", "float"))), None))
      ))
    }

  }

}
