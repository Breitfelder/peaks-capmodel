package de.tud.cs.peaks.capabilities

import java.io.BufferedWriter
import java.util.Hashtable
import java.io.FileWriter
import scala.collection.mutable.ListBuffer
import java.io.File
import org.opalj.br.Method
import scala.collection.mutable.HashMap

object AnalysisResultWriter {
  /**
   * Stores the determined capabilities to an output file
   */
  def outputCapabilities(sourcegroups: HashMap[List[String], List[(Method, List[String], String)]], outputFile: File) {
    // output file
    val file = outputFile //new File("output.txt")
    val bufferedWriter = new BufferedWriter(new FileWriter(file))

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
        for ((method, caps, subtype) <- methods) {
          var methodTypeTag = ""
          var subTypeTag = ""

          // determine methodTypeTag
          if (method.name.startsWith("<init>")) {
            methodTypeTag = "<constructor type = \""
          } else {
            methodTypeTag = "<method type = \""
          }

          // determine subTypeTag
          if (!subtype.equals("no subtype")) {
            subTypeTag = "(" + subtype + ")"
          } else {
            subTypeTag = ""
          }

          val methodTag = methodTypeTag + method.classFile.thisType.toJava + subTypeTag + "\" method = \"" + method.name + method.fullyQualifiedSignature.substring(method.fullyQualifiedSignature.indexOf("(")) + "\" />"

          bufferedWriter.write("            <source>\n")
          bufferedWriter.write("                " + methodTag + "\n")
          bufferedWriter.write("            </source>\n")

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

    // flowspec start
    bufferedWriter.write("    <flowspec>\n")
    for (flowspec <- flowspecs.toList) {
      bufferedWriter.write("        <flow from = \"" + flowspec._1 + "\" to = \"" + flowspec._2 + "\" />\n")
    }
    bufferedWriter.write("</flowspec>\n")
    bufferedWriter.write("</assmspec>\n")
    bufferedWriter.close()
  }

}