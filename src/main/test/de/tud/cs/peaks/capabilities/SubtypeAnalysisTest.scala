package de.tud.cs.peaks.capabilities

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test

import scala.collection.mutable.ListBuffer
import de.tud.cs.peaks.main.LibraryAnalysisLauncher
import java.io.File
import scala.xml.XML
import scala.xml.Node
import scala.xml.Elem
import scala.collection.mutable.HashMap
import org.xml.sax.SAXParseException

class SubtypeAnalysisTest extends JUnitSuite {

  /**
   * 1. check if a result file was written
   * 2. check if xml parsing was possible
   * 3. check for duplicate sourcegroups
   * 4. check sources for duplicates per sourcegroup
   * 5. check for duplicate sinkgroups
   * 6. check for duplicate resource
   * 7. check for duplicate - combination of resources
   * 8. check if all sourcegroups are used
   * 9. check if all sinkgroups are used
   * 10. check if test file was deleted
   */
  @Test
  def SubtypeAnalysis_FileWriter() {
    val sourcegroups: HashMap[String, Boolean] = new HashMap[String, Boolean]
    val sinkgroups: HashMap[String, Boolean] = new HashMap[String, Boolean]
    val sinkgroupsResources: HashMap[String, Boolean] = new HashMap[String, Boolean]
    val methodsWithSubtypes: HashMap[String, String] = new HashMap[String, String]

    val parameter = Array("""-cp=resources/test/test_cases/test_case_08.jar""", "-lca", "-sa", "-test")

    LibraryAnalysisLauncher.main(parameter)

    // load result file
    val resultFile = new File("resources/test/test_tmp_results/tmp_test_result.xml")

    // 1. check if a result file was written
    assert(resultFile.exists(), "->Test file not found: resources/test/test_tmp_results/tmp_test_result.xml")

    // parse result file as xml
    var resultFileXML: Elem = null
    var xmlParsing = false
    try {
      resultFileXML = XML.loadFile(resultFile)
      xmlParsing = true
    } catch {
      case pe: SAXParseException => xmlParsing = false
    }

    // 2. check if xml parsing was possible
    assert(xmlParsing, "->XML prasing was not possible for file: resources/test/test_tmp_results/tmp_test_result.xml")

    // select all sourcegroups
    val listOfSourcegroups = resultFileXML \ "interfacespec" \ "sourcegroup"

    // analyse every sourcegroup
    for (sourcegroup <- listOfSourcegroups) {
      // select all sources
      val sources = sourcegroup \ "source"
      val handleSourcegroup = (sourcegroup \ "@handle").toString()

      // 3. check for duplicate sourcegroups
      assert(!sourcegroups.contains(handleSourcegroup), "sourcegroup duplicate was found for: " + handleSourcegroup)
      sourcegroups.put(handleSourcegroup, false)

      // 4. check sources for duplicates per sourcegroup
      for (source <- sources) {
        val method = (source \ "method").head
        val methodWithSubtype = (method \ "@type").toString()

        assert(!methodsWithSubtypes.contains(methodWithSubtype))

        methodsWithSubtypes.put(methodWithSubtype, handleSourcegroup)

      }
      methodsWithSubtypes.clear()
    }

    // analyse sinkgroups
    val listOfSinkgroups = resultFileXML \ "interfacespec" \ "sinkgroup"

    for (sinkgroup <- listOfSinkgroups) {
      // select all sources
      val sources = sinkgroup \ "sink"
      val handleSinkgroup = (sinkgroup \ "@handle").toString()

      // 5. check for duplicate sinkgroups
      assert(!sinkgroups.contains(handleSinkgroup), "sinkgroup duplicate was found")
      sinkgroups.put(handleSinkgroup, false)

      val sinks = sinkgroup \ "sink"

      val sinkResources = ListBuffer.empty[String]
      for (sink <- sinks) {
        val resource = ((sink \ "resource").head \ "@type").head.toString()

        // 6. check for duplicate resource
        assert(!sinkResources.contains(resource))
        sinkResources.append(resource)
      }

      val sinkgroupResources = sinkResources.toList.sorted.foldLeft("")((z, f) => z + f)
      // 7. check for duplicate - combination of resources
      assert(!sinkgroupsResources.contains(sinkgroupResources), "duplicate for combination of resources: " + sinkgroupResources)
      sinkgroupsResources.put(sinkgroupResources, false)
    }

    // analyse flowspec
    // select all sourcegroups
    val flows = resultFileXML \ "flowspec" \ "flow"

    for (flow <- flows) {
      val from = (flow \ "@from").head.toString() // sourcegroup
      val to = (flow \ "@to").head.toString() // sinkgroup

      if (sourcegroups.contains(from)) {
        //val sourcegroup = sourcegroups.get(from)
        sourcegroups.put(from, true)
      }

      if (sinkgroups.contains(to)) {
        //val sinkgroup = sinkgroups.get(to)
        sinkgroups.put(to, true)
      }

    }

    // 8. check if all sourcegroups are used
    sourcegroups.toList.foreach(isUsed => assert(isUsed._2, "->sourcgroup: " + isUsed._1 + " not used"))

    // 9. check if all sinkgroups are used
    sinkgroups.toList.foreach(isUsed => assert(isUsed._2, "->sinkgroup: " + isUsed._1 + " not used"))

    // 10. check if test file was deleted
    //assert(resultFile.delete())
  }

}