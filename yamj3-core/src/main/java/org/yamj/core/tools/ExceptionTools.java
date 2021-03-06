/*
 *      Copyright (c) 2004-2015 YAMJ Members
 *      https://github.com/organizations/YAMJ/teams
 *
 *      This file is part of the Yet Another Media Jukebox (YAMJ).
 *
 *      YAMJ is free software: you can redistribute it and/or modify
 *      it under the terms of the GNU General Public License as published by
 *      the Free Software Foundation, either version 3 of the License, or
 *      any later version.
 *
 *      YAMJ is distributed in the hope that it will be useful,
 *      but WITHOUT ANY WARRANTY; without even the implied warranty of
 *      MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *      GNU General Public License for more details.
 *
 *      You should have received a copy of the GNU General Public License
 *      along with YAMJ.  If not, see <http://www.gnu.org/licenses/>.
 *
 *      Web: https://github.com/YAMJ/yamj-v3
 *
 */
package org.yamj.core.tools;

import javax.persistence.LockTimeoutException;
import javax.persistence.OptimisticLockException;
import javax.persistence.PessimisticLockException;
import org.hibernate.StaleStateException;
import org.hibernate.dialect.lock.LockingStrategyException;
import org.hibernate.exception.LockAcquisitionException;
import org.springframework.dao.ConcurrencyFailureException;
import org.yamj.api.common.exception.ApiException;
import org.yamj.api.common.exception.ApiExceptionType;

/**
 * Exception tools
 */
public final class ExceptionTools {

    private ExceptionTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static boolean isLockingError(Exception e) {
        final boolean result;
        if (e == null) {
            result = false;
        } else if (e instanceof ConcurrencyFailureException) {
            result = true;
        } else if (e instanceof LockingStrategyException) {
            result = true;
        } else if (e instanceof StaleStateException) {
            result = true;
        } else if (e instanceof OptimisticLockException) {
            result = true;
        } else if (e instanceof PessimisticLockException) {
            result = true;
        } else if (e instanceof LockTimeoutException) {
            result = true;
        } else if (e instanceof LockAcquisitionException) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }
    
    public static boolean is404(ApiException ex) {
        return ApiExceptionType.HTTP_404_ERROR.equals(ex.getExceptionType());
    }
}
