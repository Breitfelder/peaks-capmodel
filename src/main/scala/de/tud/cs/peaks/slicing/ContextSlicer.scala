package de.tud.cs.peaks.slicing

import java.net.URL

import de.tud.cs.peaks.capabilities.Capability
import org.opalj.br.{ClassFile, Method}
import org.opalj.br.analyses.Project

object ContextSlicer extends Slicer with ContextSlicing {
  override def title: String = "Slices a library down to methods by its usage in an application"

  override def description: String =
    "Produces a library slice based on its usage context."

  def computeSlice(project: Project[URL], caps : Set[Capability], appContext : Set[String]) : Map[ClassFile, Set[Method]] = sliceByContext(project, appContext)

}
