package de.fau.wisebed.experimentClient

import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import scala.tools.nsc._
import scala.tools.nsc.interpreter.IMain
import java.util.jar.Manifest
import scala.collection.mutable.Buffer
import scala.tools.nsc.interpreter.Results
import java.io.StringWriter
import java.io.PrintWriter
import org.slf4j.LoggerFactory

trait Foo {

}

object Client {

	def main(args: Array[String]) {
		val log = LoggerFactory.getLogger(this.getClass)


		
		

		de.fau.wisebed.util.Logging.setDefaultLogger

		if (args.length != 1) {
			printf("Wrong number of arguments.\n")
			sys.exit(1)
		}

		val f = new File(args(0));

		if (!f.exists()) {
			printf("Config file not found.")
		}

		val input = new BufferedReader(new FileReader(f))

		
		//Get Jarpath
		val jarfile = this.getClass.getProtectionDomain.getCodeSource.getLocation.getPath
		val jarpath = jarfile.take(jarfile.lastIndexOf("/") + 1) 

		//Get classpath from Manifest
		val resources = getClass.getClassLoader.getResources("META-INF/MANIFEST.MF");
		val cpath = Buffer[String]()

		//Get classpath from Manifest 
		while (resources.hasMoreElements()){
			val manifest = new Manifest(resources.nextElement().openStream());
			val attr = manifest.getMainAttributes.getValue("Class-Path")
			//Convert to absolut paths
			if (attr != null) {				
				cpath ++= attr.split(" ").map(p => {"file:" + {if(p(1) == '/') "" else jarpath} +  p })
			}
		}
		
		
		val settings = new Settings
		settings.bootclasspath.value = cpath.mkString(java.io.File.pathSeparator)
		settings.usejavacp.value = true

		val ew = new StringWriter
		val pw = new PrintWriter(ew, true)
		val sI = new IMain(settings, pw)

		//Add imports
		sI.addImports(
			"de.fau.wisebed.experimentClient.ExpClientPredef",
			"de.fau.wisebed.experimentClient.ExpClientPredef._",
			"de.fau.wisebed.messages._",
			"de.fau.wisebed.wrappers._",
			"org.slf4j.LoggerFactory",
			"de.fau.wisebed._");
		sI.interpret("val log = LoggerFactory.getLogger(\"Script\")\n")

		var str: String = null
		val lines = scala.io.Source.fromFile(f).mkString

		sI.interpret(lines) match {
			case Results.Error => {
				pw.flush
				log.error("Compile Error\n" + ew.toString)
				sys.exit(5)
			}
			case Results.Incomplete =>
			case Results.Success =>
		}

		println("\n\nDone\n\n");

	}

}
