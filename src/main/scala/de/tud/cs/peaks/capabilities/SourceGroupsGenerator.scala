package de.tud.cs.peaks.capabilities

import org.opalj.br.Method
import java.util.Hashtable
import scala.collection.mutable.HashMap

object SourceGroupsGenerator {

  var set = Array("FS", "GUI", "NET", "SYSTEM", "CLIPBOARD").toList
  var tmpCap = List[String]()
  var capGroups = HashMap.empty[List[String], List[AnalysisResult]]

  def getSourceGroups(): HashMap[List[String], List[AnalysisResult]] = {
    init()
    return capGroups
  }

  def getSourceGroups(set: List[String]): HashMap[List[String], List[AnalysisResult]] = {
    this.set = set
    init()
    return capGroups
  }

  def init() {
    var subset = List[String]()
    capGroups.clear()
    for (cap <- set.sorted) {

      subset = List[String]() // refresh for every iteration
      subset = subset.::(cap.intern())

      capGroups.put(subset, List[AnalysisResult]())

      power(cap.intern(), subset)

    }
  }

  def power(cap: String, caps: List[String]) {
    for (addCap <- set.sorted) {

      if (!caps.contains(addCap)) {
        val subset = caps.::(addCap.intern()).sorted

        capGroups.put(subset //    .sorted.foldLeft("") { (z, e) => if (z.equals("")) { e } else { z + ";" + e } }
        , List[AnalysisResult]())

        if (subset.size < set.size) {
          power(cap.intern(), subset)
        }
      }
    }
  }
}