/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package oap.application;

import oap.json.Binder;
import oap.util.Strings;

import java.net.URL;
import java.util.Map;

public class ModuleConfiguration extends Configuration<Module> {
    public ModuleConfiguration() {
        super( Module.class, "oap-module" );
    }

    public Module fromHocon( URL hocon, Map<String, Map<String, Object>> config ) {
        return fromHocon( Strings.readString( hocon ), config );
    }

    public Module fromHocon( String hocon, Map<String, Map<String, Object>> config ) {
        Module module = super.fromHocon( hocon );

        module.services
            .entrySet()
            .stream()
            .filter( e -> config.containsKey( e.getKey() ) )
            .forEach( e -> Binder.hocon.update( e.getValue(), config.get( e.getKey() ) ) );

        return module;
    }

}
