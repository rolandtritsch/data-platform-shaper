package it.agilelab.witboost.ontology.manager.uservice.api.intepreter

import cats.*
import cats.data.*
import cats.effect.*
import cats.syntax.all.*
import com.typesafe.scalalogging.StrictLogging
import it.agilelab.witboost.ontology.manager.domain.model.l0
import it.agilelab.witboost.ontology.manager.domain.model.l0.EntityType
import it.agilelab.witboost.ontology.manager.domain.model.l1.{
  SpecificTrait,
  given
}
import it.agilelab.witboost.ontology.manager.domain.model.schema.*
import it.agilelab.witboost.ontology.manager.domain.service.intepreter.{
  InstanceManagementServiceInterpreter,
  TypeManagementServiceInterpreter
}
import it.agilelab.witboost.ontology.manager.uservice.Resource.CreateTypeResponse
import it.agilelab.witboost.ontology.manager.uservice.definitions.{
  ValidationError,
  EntityType as IEntityType
}
import it.agilelab.witboost.ontology.manager.uservice.{Handler, Resource}

import scala.annotation.unused
import scala.language.implicitConversions
import scala.util.Try

class OntologyManagerHandler[F[_]: Async](
    tms: TypeManagementServiceInterpreter[F],
    @unused ims: InstanceManagementServiceInterpreter[F]
) extends Handler[F]
    with StrictLogging:

  override def createType(
      respond: Resource.CreateTypeResponse.type
  )(body: IEntityType): F[CreateTypeResponse] =

    val schema: Schema = body.schema

    val fatherName = body.fatherName

    val traits =
      summon[Applicative[F]].pure(
        Try(
          body.traits
            .fold(Set.empty[SpecificTrait])(x =>
              x.map(str => str: SpecificTrait).toSet
            )
        ).toEither
          .leftMap(t => s"Trait ${t.getMessage} is not a Trait")
      )
    val res = for {
      ts <- EitherT(traits)
      res <- EitherT(
        fatherName
          .fold(
            tms
              .create(
                l0.EntityType(
                  body.name,
                  ts,
                  schema,
                  None
                )
              )
          )(fn =>
            tms
              .create(
                l0.EntityType(
                  body.name,
                  ts,
                  schema,
                  None
                ),
                fn
              )
          )
          .map(_.leftMap(_.getMessage))
      )
    } yield res

    res.value
      .map {
        case Left(error) => respond.BadRequest(ValidationError(Vector(error)))
        case Right(_)    => respond.Ok("OK")
      }
      .onError(t =>
        summon[Applicative[F]].pure(logger.error(s"Error: ${t.getMessage}"))
      )
  end createType

  override def readType(respond: Resource.ReadTypeResponse.type)(
      name: String
  ): F[Resource.ReadTypeResponse] =

    val res = for {
      et <- tms
        .read(name)
        .map(_.leftMap(_.getMessage))
    } yield et

    res
      .map {
        case Left(error) => respond.BadRequest(ValidationError(Vector(error)))
        case Right(entityType) => respond.Ok(entityType)
      }
      .onError(t =>
        summon[Applicative[F]].pure(logger.error(s"Error: ${t.getMessage}"))
      )
  end readType
end OntologyManagerHandler
