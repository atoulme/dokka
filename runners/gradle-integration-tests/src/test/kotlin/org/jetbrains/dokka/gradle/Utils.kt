package org.jetbrains.dokka.gradle

import com.intellij.rt.execution.junit.FileComparisonFailure
import java.io.File
import java.io.IOException
import java.nio.file.FileVisitResult
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.SimpleFileVisitor
import java.nio.file.attribute.BasicFileAttributes


fun File.writeStructure(builder: StringBuilder, relativeTo: File = this, spaces: Int = 0) {

    builder.append(" ".repeat(spaces))
    val out = if (this != relativeTo) this.relativeTo(relativeTo) else this

    builder.append(out)
    if (this.isDirectory) {
        builder.appendln(":")
        this.listFiles().sortedBy { it.name }.forEach { it.writeStructure(builder, this, spaces + 4) }
    } else {
        builder.appendln()
    }
}

fun File.printStructure() {
    print(buildString { this@printStructure.writeStructure(this) })
}

fun assertEqualsIgnoringSeparators(expectedFile: File, output: String) {
    if (!expectedFile.exists()) expectedFile.createNewFile()
    val expectedText = expectedFile.readText().replace("\r\n", "\n")
    val actualText = output.replace("\r\n", "\n")

    if (expectedText != actualText)
        throw FileComparisonFailure("", expectedText, actualText, expectedFile.canonicalPath)
}


fun assertEqualsIgnoringSeparators(expectedFile: File, actualFile: File?) {
    val actualText = if (actualFile != null && actualFile.exists()) actualFile.readText() else ""
    assertEqualsIgnoringSeparators(expectedFile, actualText)
}


class CopyFileVisitor(private var sourcePath: Path?, private val targetPath: Path) : SimpleFileVisitor<Path>() {

    @Throws(IOException::class)
    override fun preVisitDirectory(dir: Path,
                                   attrs: BasicFileAttributes): FileVisitResult {
        if (sourcePath == null) {
            sourcePath = dir
        } else {
            Files.createDirectories(targetPath.resolve(sourcePath?.relativize(dir)))
        }
        return FileVisitResult.CONTINUE
    }

    @Throws(IOException::class)
    override fun visitFile(file: Path,
                           attrs: BasicFileAttributes): FileVisitResult {
        Files.copy(file, targetPath.resolve(sourcePath?.relativize(file)))
        return FileVisitResult.CONTINUE
    }
}

fun Path.copy(to: Path) {
    Files.walkFileTree(this, CopyFileVisitor(this, to))
}

