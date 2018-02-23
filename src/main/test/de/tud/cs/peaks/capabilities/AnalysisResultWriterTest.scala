package de.tud.cs.peaks.capabilities

import org.junit.Test
import org.scalatest.junit.JUnitSuite
import java.io.StringWriter
import scala.collection.mutable.ListBuffer

class AnalysisResultWriterTest extends JUnitSuite {

  @Test
  def subTypeSetTest() {
    val map = Map(
      0 -> Map(0 -> "Writer", 1 -> "FileWriter", 2 -> "StringWriter"),
      1 -> Map(0 -> "Network", 1 -> "TCP-Network"),
      2 -> Map(0 -> "Int"))

    val stringWriter = new StringWriter()
    val analysisResultWriter = new AnalysisResultWriter(stringWriter)

    val testResult = analysisResultWriter.subTypeSet(0, map, ListBuffer.empty[List[(Int, String)]], List[(Int, String)]())

    println(testResult.toList.mkString("\n"))
  }

}