package de.tud.cs.peaks.main

import java.io.File
import java.net.URL

import org.opalj.br.ObjectType
import org.opalj.br.analyses._

/**
  * Created by benhermann on 28.09.16.
  */
object JarStatistics  extends AnalysisExecutor with OneStepAnalysis[URL, Stats] {
  override val analysis: Analysis[URL, ReportableAnalysisResult] = this

  override def doAnalyze(project: Project[URL], parameters: Seq[String], isInterrupted: () => Boolean): Stats = {
    def toJarSource(u: URL): String = u.toString().substring("jar:file:".length, u.toString().indexOf("!"))

    def printToFile(f: java.io.File)(op: java.io.FileWriter => Unit) {
      val p = new java.io.FileWriter(f, true)
      try { op(p) } finally { p.close() }
    }

    var result : String = ""

    //result = result.concat(project.classFilesWithSources.flatMap(e => e._2.toString).toSet.mkString(""))
    //result = result.concat(",")

    result = result.concat(project.classFilesCount.toString)
    result = result.concat(",")
    result = result.concat(project.methodsCount.toString)

    val fullJarName = toJarSource(project.source(ObjectType(project.allProjectClassFiles.toList.apply(0).fqn)).get)


    // TODO output
    // original: printToFile(new File("/Users/benhermann/Desktop/stats.csv")) { p => p.write(fullJarName.concat(",").concat(result).concat("\n"))}
    printToFile(new File("stats.csv")) { p => p.write(fullJarName.concat(",").concat(result).concat("\n"))}

    return new Stats(result)
  }
}

class Stats (val message : String) extends ReportableAnalysisResult {
  override def toConsoleString: String = message
}
