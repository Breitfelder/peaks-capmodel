# PEAKS: Capability Analysis for Java Libraries (peaks-capmodel)

###  Abstract
Developing software from reusable libraries lets developers face a security dilemma:Either be efficient and reuse libraries as they are or inspect them, know about their resource usage, but possibly miss deadlines as reviews are a time consuming process. Here, we propose a novel capability inference mechanism for libraries written in Java. It uses a coarse-grained capability model for system resources that can be presented to developers. We found that the capability inference agrees on most expectations towards capabilities that can be derived from project documentation. Moreover, our approach can find capabilities that cannot be discovered using project documentation. It is thus a helpful tool for developers mitigating the aforementioned dilemma.

### Requirement

* Java 1.8
* Scala 2.11
* SBT 0.13
* OPAL (http://www.opal-project.de/) - dependency managed by sbt

### Getting the Source Code

If you want to install the latest version from git, clone the repository with

    git clone https://github.com/stg-tud/peaks-capmodel.git
  
### Compiling and packaging

If you want to compile and package the source code, enter: 

    sbt compile
    sbt assembly

Afterwards, you will find the compiled application as well as a runnable JAR in the target/scala-2.11 folder.

### Cleaning 

After pulling new sources you might need to execute:

    sbt clean clean-files update copyResources

### Running the application

Copy the PEAKS_JavaCapAnalysis.jar file from folder target/scala-2.11 to the root folder of your clone (where the resource folder is located). Then run it using Java.

    mv target/scala-2.11/PEAKS_JavaCapAnalysis.jar .
    java -jar PEAKS_JavaCapAnalysis.jar -cp=<targetJAR or targetFolder>

If you pass a folder to the analysis every class file and jar in this folder will be included to the analysis.
If you start the analysis, there will be a menu. 

    [1] Start capability analysis for libraries.
    [2] Sliced capability analysis for projects.
    [3] Help.

The normal usage is the first option. If you only provide a project it will print capability set to the command line. You could specify other parameters if you are only interested in some capabilities or if you want get the methods which transitively use certain capabilities. Use the third option to get an overview over all available parameters.
This is the complete list of all parameters that are available:

    [-cp=<Directories or JAR/class files> (If no class path is specified the current folder is used.)]
    [-libcp=<Directories or JAR/class files>]
    [ -lm ] - All found methods with capabilities gets listed.
    [ -CL ] - Print all methods with the CLASSLOADING capability.
    [ -CB ] - Print all methods with the CLIPBOARD capability.
    [ -DB ] - Print all methods with the DEBUG capability.
    [ -FS ] - Print all methods with the FS capability.
    [ -GU ] - Print all methods with the GUI capability.
    [ -IN ] - Print all methods with the INPUT capability.
    [ -OS ] - Print all methods with the OS capability.
    [ -NT ] - Print all methods with the NET capability.
    [ -PR ] - Print all methods with the PRINT capability.
    [ -RF ] - Print all methods with the REFLECTION capability.
    [ -SC ] - Print all methods with the SECURITY capability.
    [ -SD ] - Print all methods with the SOUND capability.
    [ -SY ] - Print all methods with the SYSTEM capability.
    [ -UN ] - Print all methods with the UNSAFE capability.

Option 2 from the menu above is a bit more advanced. The analysis will only take care of the actually used part of the libraries in an application. So far, this works only under the assumption that all the application dependencies are packaged in the same jar as the application. As this is the common case for the deployment of most applications it should fit most development processes nicely.
