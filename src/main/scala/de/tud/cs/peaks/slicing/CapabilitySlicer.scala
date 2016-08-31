package de.tud.cs.peaks.slicing
import java.net.URL

import de.tud.cs.peaks.opalreports.CapabilityAnaylsisResult
import org.opalj.br.analyses.{Analysis, Project, ReportableAnalysisResult}

object CapabilitySlicer extends Slicer {
  override def title: String = "Slices a library down to methods within the definded capability scope."

  override def description: String =
    "Produces a library slice based on a set of desired/allowed capabilities."
}
