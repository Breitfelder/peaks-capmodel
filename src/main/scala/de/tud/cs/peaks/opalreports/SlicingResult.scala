package de.tud.cs.peaks.opalreports

import org.opalj.br.analyses.ReportableAnalysisResult

/**
  * Created by benhermann on 26/08/16.
  */
sealed abstract class SlicingResult extends ReportableAnalysisResult {}

case class CapSlicingResult(val message: String)
  extends SlicingResult {
  override def toConsoleString = message
}