/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.cli.common.arguments;

import com.sampullara.cli.Argument;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.CALL;
import static org.jetbrains.kotlin.cli.common.arguments.K2JsArgumentConstants.NO_CALL;

public class K2JSCompilerArguments extends CommonCompilerArguments {
    @Argument(value = "output", description = "Output file path")
    @ValueDescription("<path>")
    public String outputFile;

    @Argument(value = "no-stdlib", description = "Don't use bundled Kotlin stdlib")
    public boolean noStdlib;

    @Argument(value = "library-files", description = "Path to zipped library sources or kotlin files separated by commas")
    @ValueDescription("<path[,]>")
    public String[] libraryFiles;

    @Argument(value = "source-map", description = "Generate source map")
    public boolean sourceMap;

    @Argument(value = "target", description = "Generate JS files for specific ECMA version (only ECMA 5 is supported)")
    @ValueDescription("<version>")
    public String target;

    @Nullable
    @Argument(value = "main", description = "Whether a main function should be called; default '" + CALL + "' (main function will be auto detected)")
    @ValueDescription("{" + CALL + "," + NO_CALL + "}")
    public String main;

    @Argument(value = "output-prefix", description = "Path to file which will be added to the beginning of output file")
    @ValueDescription("<path>")
    public String outputPrefix;

    @Argument(value = "output-postfix", description = "Path to file which will be added to the end of output file")
    @ValueDescription("<path>")
    public String outputPostfix;

    @Override
    @NotNull
    public String executableScriptFileName() {
        return "kotlinc-js";
    }
}
