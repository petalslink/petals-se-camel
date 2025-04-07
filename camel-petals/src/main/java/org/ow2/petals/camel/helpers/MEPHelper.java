/**
 * Copyright (c) 2024-2025 Linagora
 * 
 * This program/library is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 2.1 of the License, or (at your
 * option) any later version.
 * 
 * This program/library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License
 * for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program/library; If not, see http://www.gnu.org/licenses/
 * for the GNU Lesser General Public License version 2.1.
 */
package org.ow2.petals.camel.helpers;

import java.net.URI;

import org.apache.camel.ExchangePattern;
import org.ow2.easywsdl.wsdl.api.abstractItf.AbsItfOperation.MEPPatternConstants;

public class MEPHelper {

    private MEPHelper() {
        // Utility class --> No constructor
    }

    public static final ExchangePattern fromURI2ExchangePattern(final URI uriMEP) {
        return uriMEP != null ? fromURIString2ExchangePattern(uriMEP.toASCIIString()) : null;
    }

    public static final ExchangePattern fromURIString2ExchangePattern(final String uriMEP) {
        final MEPPatternConstants tmpMEP = MEPPatternConstants.fromString(uriMEP);
        switch (tmpMEP) {
            case IN_ONLY:
                return ExchangePattern.InOnly;
            case ROBUST_IN_ONLY:
                // The Camel exchange pattern 'InOnly' is the closest from Petals Exchange pattern 'RobustInOnlyOut'
                return ExchangePattern.InOnly;
            case IN_OUT:
                return ExchangePattern.InOut;
            case IN_OPTIONAL_OUT:
                // The Camel exchange pattern 'InOut' is the closest from Petals Exchange pattern 'InOptionalOut'
                return ExchangePattern.InOut;
            default:
                return null;
        }
    }

    public static final MEPPatternConstants fromExchangePattern2MEPPatternConstants(
            final ExchangePattern exchangePattern) {
        switch (exchangePattern) {
            case InOnly:
                return MEPPatternConstants.IN_ONLY;
            case InOut:
                return MEPPatternConstants.IN_OUT;
            default:
                return null;
        }
    }
}
