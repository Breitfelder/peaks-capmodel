package de.tud.cs.peaks.slicing

import java.net.URL

import de.tud.cs.peaks.capabilities.{Capability, LibraryCapabilityAnalysis}
import de.tud.cs.peaks.opalreports.{CapSlicingResult, SlicingResult}
import org.opalj.bc.Assembler
import org.opalj.br.{ClassFile, Method, ObjectType}
import org.opalj.br.analyses.{Analysis, AnalysisExecutor, OneStepAnalysis, Project, ReportableAnalysisResult}
import org.opalj.log.GlobalLogContext
import java.io.File

import de.tud.cs.peaks.repackaging.SimpleJarFile
import org.opalj.ai.analyses.cg.{CallGraph, CallGraphFactory, ExtVTACallGraphAlgorithmConfiguration}


trait Slicer extends AnalysisExecutor with OneStepAnalysis[URL, SlicingResult] {

  override val analysis: Analysis[URL, ReportableAnalysisResult] = this

  /**
    * List with allowed analysis parameters.
    *
    * @note Subclasses should check whether they want to support all of them, if not, the field should be overwritten.
    */
  val _ALLOWED_PARAMS = Seq("-appcp", "-slice", "-CL", "-CB", "-DB", "-FS", "-GU", "-IN", "-OS", "-NT", "-PR", "-RF", "-SC", "-SD", "-SY", "-UN")

  val _PARAM_MAP = Map(
    "-CL" -> Capability.ClassLoading,
    "-CB" -> Capability.Clipboard,
    "-DB" -> Capability.Debug,
    "-FS" -> Capability.Filesystem,
    "-GU" -> Capability.GUI,
    "-IN" -> Capability.Input,
    "-OS" -> Capability.Os,
    "-NT" -> Capability.Network,
    "-PR" -> Capability.Print,
    "-RF" -> Capability.Reflection,
    "-SC" -> Capability.Security,
    "-SD" -> Capability.Sound,
    "-SY" -> Capability.System,
    "-UN" -> Capability.Unsafe)

  /**
    * @see [AnalysisExecuter#printUsage]
    */
   def printUsage = super.printUsage(GlobalLogContext)

  /**
    * @see [AnalysisExecuter#analysisSpecificParametersDescription]
    */
  override def analysisSpecificParametersDescription: String = {
    val lineSep = System.lineSeparator()
      s"[ -CL ] - Print all methods with the ${Capability.ClassLoading.shortForm()} capability.$lineSep"+
      s"[ -CB ] - Print all methods with the ${Capability.Clipboard.shortForm()} capability.$lineSep"+
      s"[ -DB ] - Print all methods with the ${Capability.Debug.shortForm()} capability.$lineSep"+
      s"[ -FS ] - Print all methods with the ${Capability.Filesystem.shortForm()} capability.$lineSep"+
      s"[ -GU ] - Print all methods with the ${Capability.GUI.shortForm()} capability.$lineSep"+
      s"[ -IN ] - Print all methods with the ${Capability.Input.shortForm()} capability.$lineSep"+
      s"[ -OS ] - Print all methods with the ${Capability.Os.shortForm()} capability.$lineSep"+
      s"[ -NT ] - Print all methods with the ${Capability.Network.shortForm()} capability.$lineSep"+
      s"[ -PR ] - Print all methods with the ${Capability.Print.shortForm()} capability.$lineSep"+
      s"[ -RF ] - Print all methods with the ${Capability.Reflection.shortForm()} capability.$lineSep"+
      s"[ -SC ] - Print all methods with the ${Capability.Security.shortForm()} capability.$lineSep"+
      s"[ -SD ] - Print all methods with the ${Capability.Sound.shortForm()} capability.$lineSep"+
      s"[ -SY ] - Print all methods with the ${Capability.System.shortForm()} capability.$lineSep"+
      s"[ -UN ] - Print all methods with the ${Capability.Unsafe.shortForm()} capability.$lineSep"
  }

  /**
    * @see [AnalysisExecuter#checkAnalysisSpecificParameters]
    */
  override def checkAnalysisSpecificParameters(parameters: Seq[String]): Traversable[String] = {
    parameters.filter { param => !_ALLOWED_PARAMS.contains(param.split("=")(0)) }
  }


  override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): SlicingResult = {
    val capFilter = _PARAM_MAP.collect{ case (key, value) if parameters.contains(key) => value }.toSet
    val enhancedCapFilter = capFilter + Capability.Native

    val appcps = parameters.collect({ case (m) if m.startsWith("-appcp=") => m.substring("-appcp=".length)})

    def verifyFile(filename: String) : Option[File] =  {
      var file = new File(filename)

      if (!file.exists())
        None
      else
        Option(file)
    }
    var appContext = appcps.map(verifyFile).flatten.map(f => f.getAbsolutePath()).toSet

    var slices = slice(project, enhancedCapFilter, appContext)

    CapSlicingResult("done")
  }


  def slice(project: Project[URL], caps : Set[Capability], appContext : Set[String]) : Set[JarFile] = {
    val slice: Map[ClassFile, Set[Method]] = computeSlice(project, caps, appContext)



    val slicedJarFiles: Set[JarFile] = sliceToJars(project, slice)

    slicedJarFiles
  }

  def computeSlice(project: Project[URL], caps : Set[Capability], appContext : Set[String]) : Map[ClassFile, Set[Method]]

  def sliceToJars(project: Project[URL], slicingInfo : Map[ClassFile, Set[Method]]): Set[JarFile] = {

    val sourceJars = slicingInfo.keys.map(c => project.source(c.thisType).get.toString)
                                     .map(source => source.substring("jar:file:".length, source.indexOf("!")))

    // read in source jars in
    val daClasses = sourceJars.map(source => (source, org.opalj.da.ClassFileReader.ClassFiles(new File(source)).map(_._1)))

    val slicedJarFiles = daClasses.map(
      jarFileName => {
        // TODO make path independent from user 
        val newJarFilename = "/Users/benhermann/Code/slicing-eval-app/slices/" + jarFileName._1.substring(jarFileName._1.lastIndexOf("/") + 1)
        val jarFile: JarFile = new SimpleJarFile(newJarFilename)
        val classFiles =
          jarFileName._2.filter(cf => (slicingInfo.exists(e => e._1.fqn == cf.thisType.asJava.replace(".", "/")))).map(
            cf => {
                val currentClassSlice: (ClassFile, Set[Method]) = slicingInfo.collectFirst({ case (c, methodSet) if c.fqn == cf.thisType.asJava.replace(".", "/") => (c, methodSet) }).get

                val filteredMethods = cf.methods.filter { m ⇒
                  implicit val cp = cf.constant_pool
                  val matches = currentClassSlice._2.exists(sm => {
                    sm.name == cp(m.name_index).toString
                  })
                  matches
                }

                val filteredCF = cf.copy(methods = filteredMethods)

                val filteredCFBytes = Assembler.apply(filteredCF)

                val fullPath = project.source(ObjectType(currentClassSlice._1.fqn)).get.toString
                val path = fullPath.substring(fullPath.indexOf("!") + 1)
                (path, filteredCFBytes)
            })
        classFiles.foreach(e => jarFile.addFile(e._1, e._2))
        jarFile.close()
        jarFile
      }
    )
    slicedJarFiles.toSet
  }


}

