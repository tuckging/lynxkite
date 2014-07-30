package com.lynxanalytics.biggraph.graph_api

import com.lynxanalytics.biggraph.graph_util

object Scripting {
  import scala.language.implicitConversions

  implicit class InstanceBuilder[IS <: InputSignatureProvider, OMDS <: MetaDataSetProvider](
      op: TypedMetaGraphOp[IS, OMDS]) {
    val builder = this
    private var currentInput = MetaDataSet()
    def apply[T <: MetaGraphEntity](
      adder: EntityTemplate[T],
      container: EntityContainer[T]): InstanceBuilder[IS, OMDS] = apply(adder, container.entity)
    def apply[T <: MetaGraphEntity](
      adder: EntityTemplate[T],
      entity: T): InstanceBuilder[IS, OMDS] = {
      currentInput = adder.set(currentInput, entity)
      this
    }
    def apply() = this

    def toInstance(manager: MetaGraphManager): TypedOperationInstance[IS, OMDS] = {
      manager.apply(op, currentInput)
    }
  }

  implicit def buildInstance[IS <: InputSignatureProvider, OMDS <: MetaDataSetProvider](
    builder: InstanceBuilder[IS, OMDS])(
      implicit manager: MetaGraphManager): TypedOperationInstance[IS, OMDS] =
    builder.toInstance(manager)

  implicit def getData(entity: EntityContainer[VertexSet])(
    implicit dataManager: DataManager): VertexSetData =
    dataManager.get(entity.entity)
  implicit def getData(entity: EntityContainer[EdgeBundle])(
    implicit dataManager: DataManager): EdgeBundleData =
    dataManager.get(entity.entity)
  implicit def getData[T](entity: EntityContainer[VertexAttribute[T]])(
    implicit dataManager: DataManager): VertexAttributeData[T] =
    dataManager.get(entity.entity)
  implicit def getData[T](entity: EntityContainer[EdgeAttribute[T]])(
    implicit dataManager: DataManager): EdgeAttributeData[T] =
    dataManager.get(entity.entity)
  implicit def getData[T](entity: EntityContainer[Scalar[T]])(
    implicit dataManager: DataManager): ScalarData[T] =
    dataManager.get(entity.entity)

  implicit def getData(entity: VertexSet)(
    implicit dataManager: DataManager): VertexSetData =
    dataManager.get(entity)
  implicit def getData(entity: EdgeBundle)(
    implicit dataManager: DataManager): EdgeBundleData =
    dataManager.get(entity)
  implicit def getData[T](entity: VertexAttribute[T])(
    implicit dataManager: DataManager): VertexAttributeData[T] =
    dataManager.get(entity)
  implicit def getData[T](entity: EdgeAttribute[T])(
    implicit dataManager: DataManager): EdgeAttributeData[T] =
    dataManager.get(entity)
  implicit def getData[T](entity: Scalar[T])(
    implicit dataManager: DataManager): ScalarData[T] =
    dataManager.get(entity)

  implicit def toInput[IS <: InputSignatureProvider, OMDS <: MetaDataSetProvider](
    op: TypedMetaGraphOp[IS, OMDS]): IS = op.inputs

  implicit def emptyInputInstance[IS <: InputSignatureProvider, OMDS <: MetaDataSetProvider](
    op: TypedMetaGraphOp[IS, OMDS])(
      implicit manager: MetaGraphManager): TypedOperationInstance[IS, OMDS] =
    manager.apply(op, MetaDataSet())

  implicit def filename(fn: String): graph_util.Filename =
    graph_util.Filename.fromString(fn)
}
