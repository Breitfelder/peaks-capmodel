scalaVersion := "2.11.6"

name := "PEAKS JavaCapAnalysis"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
		
libraryDependencies ++= Seq(
							"de.opal-project" % "abstract-interpretation-framework_2.11" % "0.0.1-SNAPSHOT" withSources() withJavadoc()
							)
							
assemblyJarName in assembly := "PEAKS_JavaCapAnalysis.jar"

mainClass in assembly := Some("de.tud.cs.peaks.main.LibraryAnalysisLauncher")