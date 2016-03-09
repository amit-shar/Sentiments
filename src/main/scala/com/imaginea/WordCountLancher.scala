package com.imaginea

import org.apache.spark.launcher.SparkLauncher

object Launcher {

  def runSparkJob(sparkHome: String, appJarPath: String, mainClass: String, sparkMaster: String) {
    val spark = new SparkLauncher().setSparkHome("/opt/software/spark-1.3.1-bin-hadoop2.6").
      setAppResource("./sentiment-assembly-1.0.jar").setMainClass("com.imaginea.WordCount").
      setDeployMode("cluster").addAppArgs("192.168.2.67", "9200", "/opt/tweetWordCount").
      setMaster(sparkMaster).launch()
    println(scala.io.Source.fromInputStream(spark.getInputStream).getLines().mkString("\n"))
    println(scala.io.Source.fromInputStream(spark.getErrorStream).getLines().mkString("\n"))

    spark.waitFor
  }
}