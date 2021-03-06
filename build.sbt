scalaVersion := "2.11.11"

name := "PEAKS JavaCapAnalysis"

resolvers += "Sonatype OSS Snapshots" at "https://oss.sonatype.org/content/repositories/snapshots"
		
libraryDependencies ++= Seq(
							"de.opal-project" % "abstract-interpretation-framework_2.11" % "0.9.0-SNAPSHOT" withSources() withJavadoc(),
							"de.opal-project" % "bytecode-creator_2.11" % "0.9.0-SNAPSHOT" withSources() withJavadoc(),
							"org.scalatest" %% "scalatest" % "3.0.4" % "test"
							)
							
assemblyJarName in assembly := "PEAKS_JavaCapAnalysis.jar"

mainClass in assembly := Some("de.tud.cs.peaks.capabilities.LibraryCapabilityAnalysis")