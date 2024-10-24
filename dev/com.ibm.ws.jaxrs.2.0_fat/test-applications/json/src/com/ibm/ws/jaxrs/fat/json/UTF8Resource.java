/*******************************************************************************
 * Copyright (c) 2019 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0/
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.ws.jaxrs.fat.json;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

@Path("/country")
public class UTF8Resource {

    @GET
    @Path("/upper")
    @Produces(MediaType.APPLICATION_JSON)
    public Country[] getCountriesUpperCase() {
        List<Country> countries = new ArrayList<Country>();
        countries.add(new Country("DK", "DANMARK"));
        countries.add(new Country("EG", "ÆGYPTEN"));
        return countries.toArray(new Country[countries.size()]);
    }

    @GET
    @Path("/lower")
    @Produces(MediaType.APPLICATION_JSON)
    public Country[] getCountriesLowerCase() {
        List<Country> countries = new ArrayList<Country>();
        countries.add(new Country("DK", "danmark"));
        countries.add(new Country("EG", "ægypten"));
        return countries.toArray(new Country[countries.size()]);
    }

    @POST
    @Path("/returnCapital")
    @Produces(MediaType.APPLICATION_JSON)
    public String returnCapital(CountryInfo countryInfo) {
        // should blow up trying to read invalid json from client
        return countryInfo.capital;
    }
}
