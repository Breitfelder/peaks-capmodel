package de.tud.cs.peaks.slicing

import java.net.URL

import de.tud.cs.peaks.capabilities.{Capability, LibraryCapabilityAnalysis}
import de.tud.cs.peaks.opalreports.{CapSlicingResult, SlicingResult}
import org.opalj.bc.Assembler
import org.opalj.br.{ClassFile, Method, ObjectType}
import org.opalj.br.analyses.{Analysis, AnalysisExecutor, OneStepAnalysis, Project, ReportableAnalysisResult}
import org.opalj.log.GlobalLogContext
import java.io.File


trait Slicer extends AnalysisExecutor with OneStepAnalysis[URL, SlicingResult] {

  override val analysis: Analysis[URL, ReportableAnalysisResult] = this

  /**
    * List with allowed analysis parameters.
    *
    * @note Subclasses should check whether they want to support all of them, if not, the field should be overwritten.
    */
  val _ALLOWED_PARAMS = Seq("-slice", "-CL", "-CB", "-DB", "-FS", "-GU", "-IN", "-OS", "-NT", "-PR", "-RF", "-SC", "-SD", "-SY", "-UN")

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
    parameters.filter { param => !_ALLOWED_PARAMS.contains(param) }
  }


  override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): SlicingResult = {

    val capFilter = _PARAM_MAP.collect{ case (key, value) if parameters.contains(key) => value }.toSet
    val enhancedCapFilter = capFilter + Capability.Native

    var slices = slice(project, enhancedCapFilter)

    // TODO: Write some slices
    CapSlicingResult("done")
  }


  def slice(project: Project[URL], caps : Set[Capability]) : Seq[JarFile] = {
    // 1. determine slicing information

    println("Desired capability footprint: " +  caps.map { x ⇒ x.shortForm() }.mkString("[", ", ", "]"))

    // 1.1 Determine current footprint

    val methodsWithCapabilities = LibraryCapabilityAnalysis.computeCapabilities(project)
    val usedCaps = methodsWithCapabilities.foldLeft(Set.empty[Capability])((res, cur) ⇒ res.++(cur._2))
    println("Currently used capabilities: (" + methodsWithCapabilities.size + " method(s)) " +  usedCaps.map { x ⇒ x.shortForm() }.mkString("[", ", ", "]"))

    // 1.2 Filter undesired methods

    val filtered = methodsWithCapabilities.filter(mcaps => (mcaps._2 -- caps).isEmpty )
    val filteredCaps = filtered.foldLeft(Set.empty[Capability])((res, cur) ⇒ res.++(cur._2))
    println("Capabilities after slicing: (" + filtered.size + " method(s)) " +  filteredCaps.map { x ⇒ x.shortForm() }.mkString("[", ", ", "]"))

    val removed = methodsWithCapabilities.filter(mcaps => (mcaps._2 -- caps).nonEmpty )
    val removedCaps = removed.foldLeft(Set.empty[Capability])((res, cur) ⇒ res.++(cur._2))
    println("Capabilities removed: (" + removed.size + " method(s)) " +  removedCaps.map { x ⇒ x.shortForm() }.mkString("[", ", ", "]"))

    removed.foreach(m => println(m._1.toJava(project.classFile(m._1))))

    val libraryMethods = project.methods().filter(m => !LibraryCapabilityAnalysis.isJclSource(m, project))

    println("Complete library method count: " + libraryMethods.size)

    val newSlice = libraryMethods.toSet -- removed.map(tuple => tuple._1).toSet
    println("New slice method count: " + newSlice.size)

    // determine source jars
    val sliceClasses = newSlice.map(m => project.classFile(m))
    val sliceSources = sliceClasses.map(c => project.source(ObjectType(c.fqn)).get.toString)
    val sourceJars = sliceSources.map(source => source.substring("jar:file:".length, source.indexOf("!")))

    // read in source jars in
    val daClasses = sourceJars.map(source => (source, org.opalj.da.ClassFileReader.ClassFiles(new File(source)).map(_._1)))

    val slice : Map[ClassFile, Set[Method]] = newSlice.groupBy(m => project.classFile(m))

    val slicedJarFiles = daClasses.map(
                          jarFileName => {
                            val jarFile : JarFile = null
                            val classFiles  =
                              jarFileName._2.map(
                                cf => {
                                  if (slice.exists(e => e._1.fqn == cf.fqn.replace(".", "/"))) {
                                    val currentClassSlice : (ClassFile, Set[Method]) = slice.collectFirst({ case (c, methodSet) if c.fqn == cf.fqn.replace(".", "/") => (c, methodSet) }).get

                                    val filteredMethods = cf.methods.filter { m ⇒
                                      implicit val cp = cf.constant_pool
                                      val matches = currentClassSlice._2.exists(sm => sm.name == cp(m.name_index))
                                      matches
                                    }
                                    val filteredCF = cf.copy(methods = filteredMethods)
                                    val filteredCFBytes = Assembler.apply(filteredCF)

                                    val fullPath = project.source(ObjectType(currentClassSlice._1.fqn)).get.toString
                                    val path = fullPath.substring(fullPath.indexOf("!") + 1)
                                    (path, filteredCFBytes)
                                  }
                                }).asInstanceOf[Seq[(String, Array[Byte])]]
                            classFiles.foreach(e => jarFile.addFile(e._1, e._2))
                            jarFile
                          }
                        )

    slicedJarFiles
  }

}

