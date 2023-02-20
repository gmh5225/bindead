/*******************************************************************************
 * Copyright (c) 2000, 2011 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Markus Schorn (Wind River Systems)
 *     Andrew Ferguson (Symbian)
 *     Anton Leherbauer (Wind River Systems)
 *     oyvind.harboe@zylin.com - http://bugs.eclipse.org/250638
 *     Jens Elmenthaler - http://bugs.eclipse.org/173458 (camel case completion)
 *     Sergey Prigogin (Google)
 *******************************************************************************/
package org.eclipse.cdt.core;

import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * CCorePlugin is the life-cycle owner of the core plug-in, and starting point for access to many
 * core APIs.
 *
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class CCorePlugin {
  public static final String PLUGIN_ID = "org.eclipse.cdt.core"; //$NON-NLS-1$

  private static ResourceBundle fgResourceBundle;

  public static String getResourceString (String key) {
    try {
      return fgResourceBundle.getString(key);
    } catch (MissingResourceException e) {
      return "!" + key + "!"; //$NON-NLS-1$ //$NON-NLS-2$
    } catch (NullPointerException e) {
      return "#" + key + "#"; //$NON-NLS-1$ //$NON-NLS-2$
    }
  }

  /**
   * @noreference This method is not intended to be referenced by clients.
   */
  public static void log (String e) {
    log(createStatus(e));
  }

  /**
   * @noreference This method is not intended to be referenced by clients.
   */
  public static void log (Throwable e) {
    String msg = e.getMessage();
    if (msg == null) {
      log("Error", e); //$NON-NLS-1$
    } else {
      log("Error: " + msg, e); //$NON-NLS-1$
    }
  }

  /**
   * @noreference This method is not intended to be referenced by clients.
   */
  public static void log (String message, Throwable e) {
    log(createStatus(message, e));
  }

  /**
   * @noreference This method is not intended to be referenced by clients.
   */
  public static IStatus createStatus (String msg) {
    return createStatus(msg, null);
  }

  /**
   * @noreference This method is not intended to be referenced by clients.
   */
  public static IStatus createStatus (String msg, Throwable e) {
    return new Status(IStatus.ERROR, PLUGIN_ID, msg, e);
  }

  /**
   * @noreference This method is not intended to be referenced by clients.
   */
  public static void log (IStatus status) {
    // NOTE: this is an alternative implementation as Eclipse would use its own logging system
    // but that would pull in too many dependencies
    Logger.getLogger("Eclipse CDT Parsers").log(Level.WARNING, status.getMessage());
  }

}
