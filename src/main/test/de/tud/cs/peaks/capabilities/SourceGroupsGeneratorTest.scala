package de.tud.cs.peaks.capabilities

import org.scalatest.junit.JUnitSuite
import org.junit.Assert._
import org.junit.Test

import scala.collection.mutable.ListBuffer

class SourceGroupsGeneratorTest extends JUnitSuite {

  @Test
  def numberOfPowerSetElements() {
    assertEquals(31, SourceGroupsGenerator.getSourceGroups().size)
    assertEquals(1, SourceGroupsGenerator.getSourceGroups(List("FS")).size)
    assertEquals(3, SourceGroupsGenerator.getSourceGroups(List("FS", "NET")).size)
    assertEquals(7, SourceGroupsGenerator.getSourceGroups(List("FS", "NET", "SYSTEM")).size)
    assertEquals(15, SourceGroupsGenerator.getSourceGroups(List("FS", "NET", "SYSTEM", "GUI")).size)
    assertEquals(31, SourceGroupsGenerator.getSourceGroups(List("FS", "GUI", "NET", "SYSTEM", "CLIPBOARD")).size)
  }

  @Test
  def checkSinglePowerSetElements() {
    val powerSet = SourceGroupsGenerator.getSourceGroups(List("FS", "GUI", "NET", "SYSTEM", "CLIPBOARD"))
    val testCases = new ListBuffer[List[String]]()

    // test cases for a power set of the five basis elements
    // FS, GUI, NET, SYSTEM, CLIPBOARD
    // ########################################################################
    testCases.+=(List("CLIPBOARD"))
    testCases.+=(List("FS"))
    testCases.+=(List("GUI"))
    testCases.+=(List("NET"))
    testCases.+=(List("SYSTEM"))

    testCases.+=(List("CLIPBOARD", "FS"))
    testCases.+=(List("CLIPBOARD", "GUI"))
    testCases.+=(List("CLIPBOARD", "NET"))
    testCases.+=(List("CLIPBOARD", "SYSTEM"))
    testCases.+=(List("FS", "GUI"))
    testCases.+=(List("FS", "NET"))
    testCases.+=(List("FS", "SYSTEM"))
    testCases.+=(List("GUI", "NET"))
    testCases.+=(List("GUI", "SYSTEM"))
    testCases.+=(List("NET", "SYSTEM"))

    testCases.+=(List("CLIPBOARD", "GUI", "NET"))
    testCases.+=(List("CLIPBOARD", "GUI", "SYSTEM"))
    testCases.+=(List("CLIPBOARD", "NET", "SYSTEM"))
    testCases.+=(List("CLIPBOARD", "FS", "GUI"))
    testCases.+=(List("CLIPBOARD", "FS", "NET"))
    testCases.+=(List("CLIPBOARD", "FS", "SYSTEM"))
    testCases.+=(List("FS", "GUI", "NET"))
    testCases.+=(List("FS", "GUI", "SYSTEM"))
    testCases.+=(List("FS", "NET", "SYSTEM"))
    testCases.+=(List("GUI", "NET", "SYSTEM"))

    testCases.+=(List("FS", "GUI", "NET", "SYSTEM"))
    testCases.+=(List("CLIPBOARD", "FS", "GUI", "NET"))
    testCases.+=(List("CLIPBOARD", "FS", "GUI", "SYSTEM"))
    testCases.+=(List("CLIPBOARD", "FS", "NET", "SYSTEM"))
    testCases.+=(List("CLIPBOARD", "GUI", "NET", "SYSTEM"))

    testCases.+=(List("CLIPBOARD", "FS", "GUI", "NET", "SYSTEM"))

    // apply test cases
    for (testCase <- testCases.toList) {
      assert(powerSet.contains(testCase))
    }

    // final check: power set only contains the 31 applied test cases
    assertEquals(31, powerSet.size)

  }

}