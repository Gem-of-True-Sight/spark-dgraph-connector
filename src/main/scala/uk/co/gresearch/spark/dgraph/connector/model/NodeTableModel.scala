package uk.co.gresearch.spark.dgraph.connector.model

import uk.co.gresearch.spark.dgraph.connector.encoder.JsonNodeInternalRowEncoder
import uk.co.gresearch.spark.dgraph.connector.executor.ExecutorProvider
import uk.co.gresearch.spark.dgraph.connector.{Chunk, GraphQl, NoPartitionMetrics, PartitionMetrics, PartitionQuery}

/**
 * Models only the nodes of a graph as a table.
 */
case class NodeTableModel(execution: ExecutorProvider,
                          encoder: JsonNodeInternalRowEncoder,
                          chunkSize: Int,
                          metrics: PartitionMetrics = NoPartitionMetrics())
  extends GraphTableModel {

  override def withMetrics(metrics: PartitionMetrics): NodeTableModel = copy(metrics = metrics)

}
