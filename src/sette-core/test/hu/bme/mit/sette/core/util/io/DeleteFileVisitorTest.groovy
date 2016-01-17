/*
 * SETTE - Symbolic Execution based Test Tool Evaluator
 *
 * SETTE is a tool to help the evaluation and comparison of symbolic execution based test input 
 * generator tools.
 *
 * Budapest University of Technology and Economics (BME)
 *
 * Authors: Lajos Cseppentő <lajos.cseppento@inf.mit.bme.hu>, Zoltán Micskei <micskeiz@mit.bme.hu>
 *
 * Copyright 2014-2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except 
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the 
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either 
 * express or implied. See the License for the specific language governing permissions and 
 * limitations under the License.
 */
package hu.bme.mit.sette.core.util.io

import java.nio.file.Files
import java.nio.file.Path

import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException

import groovy.transform.TypeChecked;

/**
 * Tests to verify that {@link DeleteFileVisitor} deletes the file tree recursively when used with
 * the {@link Files} class.
 */
@TypeChecked
class DeleteFileVisitorTest {
    Path tmpDir

    @Rule
    public ExpectedException thrown = ExpectedException.none()

    @Before
    void setUp() {
        tmpDir = Files.createTempDirectory(getClass().simpleName)
    }

    @After
    void tearDown() {
        if (Files.exists(tmpDir)) {
            Files.walk(tmpDir).forEach { Files.delete(it) }
        }

        assert !Files.exists(tmpDir)
    }

    @Test(expected = IOException)
    void testIOExceptionIfDirDoesNotExist() {
        Files.delete(tmpDir)
        Files.walkFileTree(tmpDir, new DeleteFileVisitor())
    }

    @Test
    void testDeletesDirWithOneFile() {
        Path file = Files.createFile(tmpDir.resolve('file.txt'))
        Files.walkFileTree(file, new DeleteFileVisitor())

        assert !Files.exists(file)
        assert Files.exists(tmpDir)
    }

    @Test
    void testDeletesEmptyDir() {
        Files.walkFileTree(tmpDir, new DeleteFileVisitor())

        assert !Files.exists(tmpDir)
    }

    @Test
    void testDeletesDirWithSeveralFiles() {
        List<Path> children = []
        children << Files.createFile(tmpDir.resolve('a.txt'))
        children << Files.createFile(tmpDir.resolve('b.txt'))
        children << Files.createDirectory(tmpDir.resolve('c'))

        Files.walkFileTree(tmpDir, new DeleteFileVisitor())

        assert !Files.exists(tmpDir)
        children.each { assert !Files.exists(it) }
    }

    @Test
    void testDeletesDirTree() {
        List<Path> children = []
        children << Files.createFile(tmpDir.resolve('a.txt'))
        children << Files.createFile(tmpDir.resolve('b.txt'))
        children << Files.createDirectory(tmpDir.resolve('c'))
        children << Files.createFile(tmpDir.resolve('c/d.txt'))
        children << Files.createFile(tmpDir.resolve('c/e.txt'))
        children << Files.createDirectory(tmpDir.resolve('c/f'))
        children << Files.createFile(tmpDir.resolve('c/f/g.txt'))

        Files.walkFileTree(tmpDir, new DeleteFileVisitor())

        assert !Files.exists(tmpDir)
        children.each { assert !Files.exists(it) }
    }
}
