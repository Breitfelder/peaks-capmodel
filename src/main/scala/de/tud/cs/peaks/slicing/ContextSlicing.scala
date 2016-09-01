package de.tud.cs.peaks.slicing

import java.net.URL

import org.opalj.br.{ClassFile, Method}
import org.opalj.br.analyses.Project

trait ContextSlicing {
  def sliceByContext(project: Project[URL]) : Map[ClassFile, Set[Method]] = {
    // 1. determine application



    Map()
  }
}
