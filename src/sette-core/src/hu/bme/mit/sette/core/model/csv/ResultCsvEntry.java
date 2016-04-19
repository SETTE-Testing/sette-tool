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
package hu.bme.mit.sette.core.model.csv;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;

import lombok.Data;

@Data
@JsonPropertyOrder({ "Category", "Snippet", "Tool", "Coverage", "Status", "Size", "Run",
        "Duration", "RequiredStatementCoverage" })
public final class ResultCsvEntry {
    @JsonProperty("Category")
    private String category;

    @JsonProperty("Snippet")
    private String snippet;

    @JsonProperty("Tool")
    private String tool;

    @JsonProperty("Coverage")
    private String coverage;

    @JsonProperty("Status")
    private String status;

    @JsonProperty("Size")
    private String size;

    @JsonProperty("Run")
    private String run;

    @JsonProperty("Duration")
    private String duration;

    @JsonProperty("RequiredStatementCoverage")
    private String requiredStatementCoverage;

    public static CsvSchema createSchema() {
        CsvMapper csv = new CsvMapper();
        return csv.schemaFor(ResultCsvEntry.class).withHeader();
    }
}
