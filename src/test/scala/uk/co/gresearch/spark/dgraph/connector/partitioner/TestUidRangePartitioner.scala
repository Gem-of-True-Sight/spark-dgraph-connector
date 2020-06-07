package uk.co.gresearch.spark.dgraph.connector.partitioner

import java.util.UUID

import org.scalatest.FunSpec
import uk.co.gresearch.spark.dgraph.connector.{ClusterState, Partition, Predicate, Schema, Target, UidRange}

class TestUidRangePartitioner extends FunSpec {

  describe("UidRangePartitioner") {

    val schema = Schema(Set(Predicate(s"pred1", s"type1"), Predicate(s"pred2", s"type2")))
    val clusterState = ClusterState(
      Map(
        "1" -> Set(Target("host1:9080"), Target("host2:9080")),
        "2" -> Set(Target("host3:9080"))
      ),
      Map(
        "1" -> Set("pred1"),
        "2" -> Set("pred2")
      ),
      10000,
      UUID.randomUUID()
    )

    val singleton = SingletonPartitioner(Seq(Target("host1:9080"), Target("host2:9080"), Target("host3:9080")))
    val group = GroupPartitioner(schema, clusterState)
    val alpha = AlphaPartitioner(schema, clusterState, 1)
    val pred = PredicatePartitioner(schema, clusterState, 1)

    val testPartitioners =
      Seq(
        (singleton, "singleton"),
        (group, "group"),
        (alpha, "alpha"),
        (pred, "predicates"),
      )

    val testRanges =
      Seq(
        Seq(UidRange(0, 5000), UidRange(5000, 5000)),
        Seq(UidRange(0, 2000), UidRange(2000, 2000), UidRange(4000, 2000), UidRange(6000, 2000), UidRange(8000, 2000)),
      )

    testPartitioners.foreach { case (partitioner, label) =>
      testRanges.foreach(ranges =>

        it(s"should decorate $label partitioner with factor ${ranges.length}") {
          val uidPartitioner = UidRangePartitioner(partitioner, ranges.length, clusterState.maxLeaseId)
          val partitions = partitioner.getPartitions
          val uidPartitions = uidPartitioner.getPartitions
          println(uidPartitions)
          assert(uidPartitions.length === partitions.length * ranges.length)
          val expectedPartitions = ranges.zipWithIndex.flatMap { case (range, idx) =>
            partitions.map(partition => Partition(partition.targets.rotate(idx), partition.predicates, Some(range)))
          }
          assert(uidPartitions === expectedPartitions)
        }

      )
    }

    testPartitioners.foreach{ case (partitioner, label) =>

      it(s"should noop $label partitioner with partitioning factor one") {
        val uidPartitioner = UidRangePartitioner(partitioner, 1, clusterState.maxLeaseId)
        assert(uidPartitioner.getPartitions === partitioner.getPartitions)
      }

    }

    it("should fail on null partitioner") {
      assertThrows[IllegalArgumentException] {
        UidRangePartitioner(null, 1, 1000)
      }
    }

    it("should fail on negative or zero partitioning factor") {
      assertThrows[IllegalArgumentException] {
        UidRangePartitioner(singleton, -1, 1000)
      }
      assertThrows[IllegalArgumentException] {
        UidRangePartitioner(singleton, 0, 1000)
      }
    }

    it("should fail on negative or zero max uids") {
      assertThrows[IllegalArgumentException] {
        UidRangePartitioner(singleton, 1, -1)
      }
      assertThrows[IllegalArgumentException] {
        UidRangePartitioner(singleton, 1, 0)
      }
    }

    it("should fail on decorating uid partitioner") {
      val uidPartitioner = UidRangePartitioner(singleton, 2, 1000)
      assertThrows[IllegalArgumentException] {
        UidRangePartitioner(uidPartitioner, 1, 1000)
      }
    }

  }

}
