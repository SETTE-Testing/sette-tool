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
 * Tests for {@link PathValidator}.
 */
@CompileStatic
class PathValidatorTest {
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

    @Test(expected = NullPointerException)
    void testConstructorThrowsExceptionIfNull() {
        new PathValidator(null)
    }

    @Test
    void testTypes() {
        assert new PathValidator(nonExistent).type(PathType.NONEXISTENT).isValid()
        assert !new PathValidator(nonExistent).type(PathType.DIRECTORY).isValid()
        assert !new PathValidator(nonExistent).type(PathType.REGULAR_FILE).isValid()

        assert !new PathValidator(tmpDir).type(PathType.NONEXISTENT).isValid()
        assert new PathValidator(tmpDir).type(PathType.DIRECTORY).isValid()
        assert !new PathValidator(tmpDir).type(PathType.REGULAR_FILE).isValid()

        assert !new PathValidator(tmpFile).type(PathType.NONEXISTENT).isValid()
        assert !new PathValidator(tmpFile).type(PathType.DIRECTORY).isValid()
        assert new PathValidator(tmpFile).type(PathType.REGULAR_FILE).isValid()
    }

    @Test(expected = NullPointerException)
    void testTypeThrowsExceptionIfNull() {
        new PathValidator(tmpDir).type(null)
    }

    @Test
    void testPermissions() {
        // dir is rwx by the creator on both Windows and Linux
        new PathValidator(tmpDir).readable(true).writable(true).executable(true).validate()
    }

    @Test
    void testExtension() {
        assert new PathValidator(tmpDir).extension(null).isValid()
        assert !new PathValidator(tmpDir).extension('txt').isValid()

        assert !new PathValidator(tmpFile).extension(null).isValid()
        assert new PathValidator(tmpFile).extension('txt').isValid()
    }

    @Test
    void testReturnValues() {
        PathValidator v = new PathValidator(tmpDir)

        assert v.type(PathType.DIRECTORY).is(v)
        assert v.type(PathType.REGULAR_FILE).is(v)
        assert v.readable(true).is(v)
        assert v.readable(false).is(v)
        assert v.writable(true).is(v)
        assert v.writable(false).is(v)
        assert v.executable(true).is(v)
        assert v.executable(false).is(v)
        assert v.extension(null).is(v)
        assert v.extension('txt').is(v)
    }

    @Test
    void testForNonexistent() {
        assert PathValidator.forNonexistent(nonExistent).isValid()
        assert !PathValidator.forNonexistent(tmpDir).isValid()
        assert !PathValidator.forNonexistent(tmpFile).isValid()
    }

    @Test
    void testForDirectory() {
        assert !PathValidator.forDirectory(nonExistent, null, null, null).isValid()
        assert !PathValidator.forDirectory(tmpFile, null, null, null).isValid()

        assert PathValidator.forDirectory(tmpDir, null, null, null).isValid()
        assert PathValidator.forDirectory(tmpDir, true, true, true).isValid()
        assert !PathValidator.forDirectory(tmpDir, false, false, false).isValid()
    }

    @Test
    void testForFile() {
        assert !PathValidator.forRegularFile(nonExistent, null, null, null, null).isValid()
        assert !PathValidator.forRegularFile(tmpDir, null, null, null, null).isValid()

        assert !PathValidator.forRegularFile(tmpFile, null, null, null, null).isValid()
        assert PathValidator.forRegularFile(tmpFile, null, null, null, 'txt').isValid()
        assert !PathValidator.forRegularFile(tmpFile, true, true, true, null).isValid()
        assert PathValidator.forRegularFile(tmpFile, true, true, true, 'txt').isValid()
        assert !PathValidator.forRegularFile(tmpFile, false, false, false, null).isValid()
        assert !PathValidator.forRegularFile(tmpFile, false, false, false, 'txt').isValid()
    }
}
