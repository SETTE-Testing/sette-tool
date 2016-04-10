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
package hu.bme.mit.sette.core.validator

import groovy.transform.CompileStatic

import java.nio.file.Files
import java.nio.file.Path

import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Tests for {@link PathType}.
 */
@CompileStatic
class PathTypeTest {
    Path nonExistent
    Path tmpDir
    Path tmpFile

    @Before
    void setUp() {
        tmpDir = Files.createTempDirectory(getClass().simpleName)
        tmpFile = Files.createTempFile(getClass().simpleName, '.txt')
        nonExistent = tmpDir.resolve('non-existent')
    }

    @After
    void tearDown() {
        if (Files.exists(tmpDir)) {
            Files.walk(tmpDir).forEach { Files.delete(it) }
        }
        assert !Files.exists(tmpDir)

        if (Files.exists(tmpFile)) {
            Files.delete(tmpFile)
        }
        assert !Files.exists(tmpFile)
    }

    @Test
    void testEnumValues() {
        PathType.with {
            assert [NONEXISTENT, DIRECTORY, REGULAR_FILE] as Set == values() as Set

            values().each { PathType v ->
                assert v == valueOf(v as String)
            }
        }
    }

    @Test
    void testTest() {
        assert PathType.NONEXISTENT.test(nonExistent)
        assert !PathType.NONEXISTENT.test(tmpDir)
        assert !PathType.NONEXISTENT.test(tmpFile)

        assert !PathType.DIRECTORY.test(nonExistent)
        assert PathType.DIRECTORY.test(tmpDir)
        assert !PathType.DIRECTORY.test(tmpFile)

        assert !PathType.REGULAR_FILE.test(nonExistent)
        assert !PathType.REGULAR_FILE.test(tmpDir)
        assert PathType.REGULAR_FILE.test(tmpFile)
    }

    @Test(expected = NullPointerException)
    void tessTestThrowsExceptionIfNull() {
        PathType.NONEXISTENT.test(null)
    }

    @Test
    void testForPath() {
        assert PathType.forPath(nonExistent) == PathType.NONEXISTENT
        assert PathType.forPath(tmpDir) == PathType.DIRECTORY
        assert PathType.forPath(tmpFile) == PathType.REGULAR_FILE
    }

    @Test(expected = NullPointerException)
    void testForPathThrowsExceptionIfNull() {
        PathType.forPath(null)
    }
}
