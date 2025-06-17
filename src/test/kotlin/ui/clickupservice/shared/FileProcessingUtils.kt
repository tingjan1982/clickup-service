package ui.clickupservice.shared

import java.io.File

object FileProcessingUtils {

    const val CSV_DIR = "/Users/joelin/Downloads/import-source"

    fun getCsvFile(dir: File): File {
        val csvFile = dir.listFiles { file -> file.extension == "csv" && !file.nameWithoutExtension.endsWith("processed") }?.firstOrNull()

        return csvFile ?: throw Exception("File does not exist or it has been mark as processed: $dir")
    }

    fun markFileAsProcessed(file: File) {

        val newFile = File(file.parentFile, "${file.nameWithoutExtension} - processed.${file.extension}")
        file.renameTo(newFile)
        println("File has been marked as processed with file suffix: ${file.nameWithoutExtension}")
    }
}