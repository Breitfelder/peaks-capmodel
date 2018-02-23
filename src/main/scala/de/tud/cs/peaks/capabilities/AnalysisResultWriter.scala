package de.tud.cs.peaks.capabilities

import java.io.BufferedWriter
import java.util.Hashtable
import java.io.FileWriter
import scala.collection.mutable.ListBuffer
import java.io.File
import org.opalj.br.Method
import scala.collection.mutable.HashMap
import java.io.Writer

class AnalysisResultWriter(val outputWriter: Writer) {

  val bufferedWriter: BufferedWriter = new BufferedWriter(outputWriter)

  /**
   * Stores the determined capabilities to an output file
   */
  def outputCapabilities(sourcegroups: HashMap[List[String], List[AnalysisResult]]) {
    // output file
    //    val file = outputFile //new File("output.txt")
    //    val

    // flowspec output
    val flowspecs = new ListBuffer[(String, String)]

    // sinkgroup handle output
    val sinkgroups = new ListBuffer[(String, List[String])]

    val keys = sourcegroups.keys

    bufferedWriter.write("<assmspec>\n")
    bufferedWriter.write("    <interfacespec>\n")

    for (key <- keys) {
      val keyString = key.sorted.foldLeft("") {
        (z, e) => if (z.equals("")) { e } else { z + "-" + e }
      }
      val methods = sourcegroups(key)

      // sourcegroup handle start if methods.size > 0
      if (methods.size > 0) {
        val sourcegroup = "sourcegroup-" + keyString // name of sourcegroup handle
        bufferedWriter.write("        <sourcegroup handle = \"" + sourcegroup + "\">\n")

        // output of source tags
        for ((classFile, list) <- methods.groupBy(m => m.analyzedMethod.classFile)) {

          var constructors = Map.empty[String, MethodRepresentation]
          for (l <- list.filter(le => le.analyzedMethod.isConstructor && le.paramIndex > -1)) {
            var set = Set(l.subType)
            var constructor = new MethodRepresentation(l.analyzedMethod.classFile.thisType.fqn, l.analyzedMethod, scala.collection.mutable.Map(l.paramIndex -> set))
            if (constructors.contains(l.analyzedMethod.classFile.thisType.fqn + l.analyzedMethod.toJava)) {
              constructor = constructors(l.analyzedMethod.classFile.thisType.fqn + l.analyzedMethod.toJava)
              if (constructor.subTypeMapping.contains(l.paramIndex)) {
                set = constructor.subTypeMapping(l.paramIndex)
                set += l.subType
              }
              constructor.subTypeMapping += l.paramIndex -> set
            }
            constructors += (l.analyzedMethod.classFile.thisType.fqn + l.analyzedMethod.toJava) -> constructor
          }

          for (constructor <- constructors) {
            getXMLRepresentationOfConstructors(bufferedWriter, constructor._2)
          }

          for (a <- list.filter(p => !p.analyzedMethod.isConstructor)) {

          }

        }

        bufferedWriter.write("        </sourcegroup>\n") // sourcegroup handle end

        // store sinkgroup handle information temporarily for later output
        val sinkgroupHandle = "sinkgroup-" + keyString
        sinkgroups.+=((sinkgroupHandle, key))

        // store flowspec information temporarily for later output
        flowspecs.+=((sourcegroup, sinkgroupHandle))

      }

    }

    // sinkgroup handle start
    for ((sinkgroupHandle, caps) <- sinkgroups) {
      bufferedWriter.write("        <sinkgroup handle = \"" + sinkgroupHandle + "\">\n")

      for (cap <- caps) {
        bufferedWriter.write("            <sink>\n")
        bufferedWriter.write("                <resource type = \"" + CapMapper.mapCap(cap) + "\" />\n")
        bufferedWriter.write("            </sink>\n")

      }

      bufferedWriter.write("        </sinkgroup>\n")
    } // sinkgroup handle end

    bufferedWriter.write("</interfacespec>\n") // inferfacespec end

    // flowspec start22
    bufferedWriter.write("    <flowspec>\n")
    for (flowspec <- flowspecs.toList) {
      bufferedWriter.write("        <flow from = \"" + flowspec._1 + "\" to = \"" + flowspec._2 + "\" />\n")
    }
    bufferedWriter.write("    </flowspec>\n")
    bufferedWriter.write("</assmspec>\n")
    bufferedWriter.close()
  }

  def getXMLRepresentationOfConstructors(bufferedWriter: BufferedWriter, methodRepresentation: MethodRepresentation) {
    var methodTypeTag = ""
    var subTypeTag = ""

    // constructor handling
    // ########################################################################
    if (methodRepresentation.method.isConstructor) {
      methodTypeTag = "<constructor type = \""
      //subTypeTag = methodRepresentation.subTypeMapping

      var map = Map.empty[Int, Map[Int, String]]
      for (ptype <- methodRepresentation.method.parameterTypes) {
        var subTypeSet = Map.empty[Int, String]
        if (methodRepresentation.subTypeMapping.contains(methodRepresentation.method.parameterTypes.indexOf(ptype))) {
          subTypeSet = methodRepresentation.subTypeMapping(methodRepresentation.method.parameterTypes.indexOf(ptype)).zipWithIndex.map(st => st._2 -> st._1).toMap
        } else {
          subTypeSet = Map(0 -> ptype.toJava)
        }
        map += methodRepresentation.method.parameterTypes.indexOf(ptype) -> subTypeSet
      }

      if (map.nonEmpty) {
        val paramCombis = subTypeSet(0, map, ListBuffer.empty[List[(Int, String)]], List[(Int, String)]())

        for (paramCombi <- paramCombis) {
          val methodTag = methodTypeTag + methodRepresentation.method.classFile.thisType.toJava + "(" + paramCombi.map(m => m._2).mkString(", ") + ")" + "\" />"
          writeSource(methodTag)
        }
      }
    } else {
      // non constructor handling
      // ######################################################################
      methodTypeTag = "<method type = \""
    }

    // determine subTypeTag
    //    if (!subtype.equals("no subtype")) {
    //      subTypeTag = "(" + subtype + ")"
    //    } else {
    //      subTypeTag = ""
    //    }

    //val methodTag = methodTypeTag + method.classFile.thisType.toJava + subTypeTag + "\" method = \"" + method.name + method.fullyQualifiedSignature.substring(method.fullyQualifiedSignature.indexOf("(")) + "\" />"

  }

  def writeSource(methodTag: String) {
    bufferedWriter.write("            <source>\n")
    bufferedWriter.write("                " + methodTag + "\n")
    bufferedWriter.write("            </source>\n")
  }

  def subTypeSet(indexX: Int, map: Map[Int, Map[Int, String]], paramCombi: ListBuffer[List[(Int, String)]], paramCombiList: List[(Int, String)]): ListBuffer[List[(Int, String)]] = {
    for (entry <- map(indexX)) {
      if (map.contains(indexX + 1)) {
        subTypeSet(indexX + 1, map, paramCombi, paramCombiList ++ List((indexX, entry._2)))
      } else {
        paramCombi += (paramCombiList ++ List((indexX, entry._2)))
      }
    }
    paramCombi
  }

  // constructor -> method
  class MethodRepresentation(val fqn: String, val method: Method, val subTypeMapping: scala.collection.mutable.Map[Int, Set[String]])

}