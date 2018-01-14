Zusaetzliche Subtyp-Analyse fuer PEAKS: Capability Analysis for Java Libraries (peaks-capmodel)
###############################################################################################

Die Subtyp-Analyse erlaubt es nach der Standardanalyse eine zusaetzliche Subtypeanalyse
durchzufuehren. Dabei wird der Sourcecode der einzelenen Ergebnismethoden, die durch die
Standardanalyse ermittelt wurden, untersucht. Dies geschieht wie folgt:

(1) Die Subtyp-Analyse muss beim Aufrufen von PEAKS mit dem Parameter
"-sa" aktiviert werden.

(2) Ist die Subtyp-Analyse aktiviert worden, wird sie nach der Standardanalyse
ausgefuehrt. Die Programmlogik fuer die Subtypanalyse befindet sich in der Klasse 
SubtypeCapabilityAnalysis. Die Analyse kann mit der Methode startAnalysis() gestartet werden.

(3) Dazu wird der Sourcecode aller Methode, die in der Standardanalyse ermittelt wurden
auf Methodenaufrufe untersucht.
Dabei werden folgende JVM-Instruktionen beruecksichtigt:
- INVOKEINTERFACE
- INVOKESPECIAL
- INVOKESTATIC
- INVOKEVIRTUAL (funktioniert noch nicht wegen Typen-Inkompatibilitaet)

(4) Wurde ein Methodenaufruf gefunden wird ueberprueft, ob die aufgerufene Methode
zu einer Klasse mit Subtypen gehoert. Ist dies der Fall werden alle Methoden-Objekte
ermittelt die zu den zugehoerigen Subtypklassen gehoeren. Dies geschieht mit Hilfe von:
	project.classHierarchy.allSubclassTypes(declaringClass, false)
	
Dabei werden die Subtypklassen ueber ihre fqn identifiziert und die Methoden-Objekte
ueber ein ID die wie folgt aufgebaut ist:
Rueckgabewert + Methodenname + (Parameter)

(5) Im folgenden Schritt werden dann die Capabilities der identifizierten
Subtypen ermittelt. Diese Capabilities werden dann zusammen mit den urspruenglichen
Capabilities zu einer neuen Menge von Capabilities zusammengefasst. Diese neue Menge
wird dann mit der Urspruenglich aufrufenden Methode verknuepft und als ein neuer 
Eintag in der Analyseergebnismenge abgespeichert. Des Weiteren wird dem Ergebnis noch
die Bezeichnung des zugehoeringen Subtypes hinzugefuegt.

Um die Ergebnisse aus der Standardanalyse und der Subtyp-Analyse besser zusammenfuehren
zu koennen haben alle Ergebnisse ein Attribute Subtyp. Die Ergebnissen aus der
Standardanalyse, bei welchen es sich um keinen Subtyp handelt, sind mit "no subtyp"
gekenzeichnet. Diese String ist beschriebenen Attribut hinterlegt.

(6) Nach der Analyse werden die Ergebnisse in eine Textdatei geschrieben.
Dies geschieht mit Hilfe von: AnalysisResultWriter.outputCapabilities(...)

Vor der Ausgabe werden die Ergebnisse noch nach Capabilities gruppiert und 
dann in diesen Gruppen ausgegeben. Die Gruppierung erfolgt mit Hilfe einer Hashtable,
die als Schluessel die zu einer Methode gehoereden Capabilities verwendet.
	

