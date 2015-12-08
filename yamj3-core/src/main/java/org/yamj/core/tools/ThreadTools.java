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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

public final class ThreadTools {

    private ThreadTools() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    public static void waitForTermination(ExecutorService executor) {
        executor.shutdown();

        // run until all workers have finished
        while (!executor.isTerminated()) {
            try {
                TimeUnit.SECONDS.sleep(5);
            } catch (InterruptedException ignore) {
                // interrupt in sleep can be ignored
            }
        }
    }
}
