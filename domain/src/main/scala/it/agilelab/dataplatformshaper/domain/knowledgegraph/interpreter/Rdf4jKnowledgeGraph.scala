package it.agilelab.dataplatformshaper.domain.knowledgegraph.interpreter

import cats.effect.Sync
import it.agilelab.dataplatformshaper.domain.knowledgegraph.KnowledgeGraph
import it.agilelab.dataplatformshaper.domain.model.NS.L3
import org.eclipse.rdf4j.model.{IRI, Resource, Statement, Value}
import org.eclipse.rdf4j.query.BindingSet

import scala.jdk.CollectionConverters.*

class Rdf4jKnowledgeGraph[F[_]: Sync](session: Session)
    extends KnowledgeGraph[F]:
  @SuppressWarnings(
    Array(
      "scalafix:DisableSyntax.defaultArgs"
    )
  )
  override def removeAndInsertStatements(
      statements: List[Statement],
      deleteStatements: List[(Resource, IRI, Value)] =
        List.empty[(Resource, IRI, Value)]
  ): F[Unit] =
    session.withTx(conn => {
      deleteStatements.foreach(st => {
        conn.remove(
          st(0),
          st(1),
          st(2),
          L3
        )
      })
      conn.add(statements.asJava)
    })
  end removeAndInsertStatements

  override def evaluateQuery(query: String): F[Iterator[BindingSet]] =
    session.withTx { connection =>
      val tupledQuery = connection.prepareTupleQuery(query)
      tupledQuery.evaluate().iterator().asScala
    }
  end evaluateQuery
end Rdf4jKnowledgeGraph
