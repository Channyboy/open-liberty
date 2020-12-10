/*******************************************************************************
 * Copyright (c) 2017, 2020 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package io.openliberty.microprofile.metrics40.helper;

import org.eclipse.microprofile.metrics.MetricRegistry;

import io.openliberty.microprofile.metrics40.exceptions.NoSuchRegistryException;
import io.openliberty.microprofile.metrics40.internal.Constants;
import io.openliberty.microprofile.metrics40.internal.impl.SharedMetricRegistries;

/**
 *
 */
public class Util {

    public static SharedMetricRegistries SHARED_METRIC_REGISTRIES;

    private static MetricRegistry getRegistry(String registryName) throws NoSuchRegistryException {
        if (!Constants.REGISTRY_NAMES_LIST.contains(registryName)) {
            throw new NoSuchRegistryException();
        }
        return SHARED_METRIC_REGISTRIES.getOrCreate(registryName);
    }

}
