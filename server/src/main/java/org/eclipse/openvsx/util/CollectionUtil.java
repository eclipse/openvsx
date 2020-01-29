/********************************************************************************
 * Copyright (c) 2019 TypeFox and others
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
package org.eclipse.openvsx.util;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public final class CollectionUtil {

    private CollectionUtil() {}

    public static <S, T> List<T> map(List<S> source, Function<? super S, ? extends T> function) {
        var result = new ArrayList<T>(source.size());
        for (var s : source) {
            var t = function.apply(s);
            if (t != null)
                result.add(t);
        }
        return result;
    }

    public static <S, T> List<T> map(Iterable<S> source, Function<? super S, ? extends T> function) {
        var result = new ArrayList<T>();
        for (var s : source) {
            var t = function.apply(s);
            if (t != null)
                result.add(t);
        }
        return result;
    }

}