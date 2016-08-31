package de.tud.cs.peaks.slicing

import java.io.File

import de.tud.cs.peaks.capabilities.{Capability, CapabilitySliceAnalysis, LibraryCapabilityAnalysis}
import org.opalj.br.analyses.Project

/**
  * Created by benhermann on 25/08/16.
  */
object SlicingLauncher {
  val rtFile = new File("resources/jre_7.0_60/rt.jar")
  val rtProject = Project.apply(rtFile)

  def printMenu() : Unit = {
    val lineSep = System.getProperty("line.separator")
    println(
      s"$lineSep[1] Slice by desired capability set.$lineSep" +
        s"[2] Help."
    )
  }

  /**
    * Entry point regarding the console.
    */
  def main(args : Array[String]) {
    var validInput = false
    var userInput = 0

    if(args.contains("-help"))
      CapabilitySlicer.printUsage

    if(args.contains("-slice")) {
      userInput = 1
      validInput = true
    }

    while(!validInput){
      printMenu()
      userInput = scala.io.StdIn.readInt()
      validInput = Seq(1,2,3).contains(userInput)
    }
    val argsWithRTJar = args ++ Array("""-cp="resources/jre_7.0_60/rt.jar"""")
    userInput match {
      case 1 => CapabilitySlicer.main(argsWithRTJar)
      case 3 => CapabilitySlicer.printUsage
    }
  }
}
