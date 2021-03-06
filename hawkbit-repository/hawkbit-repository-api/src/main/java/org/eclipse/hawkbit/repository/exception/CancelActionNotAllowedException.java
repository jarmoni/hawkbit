/**
 * Copyright (c) 2015 Bosch Software Innovations GmbH and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.eclipse.hawkbit.repository.exception;

import org.eclipse.hawkbit.exception.SpServerError;
import org.eclipse.hawkbit.exception.SpServerRtException;

/**
 * Thrown if cancelation of actions is performened where the action is not
 * cancelable, e.g. the action is not active or is already a cancel action.
 *
 *
 *
 *
 */
public final class CancelActionNotAllowedException extends SpServerRtException {
    /**
    *
    */
    private static final long serialVersionUID = 1L;

    /**
     * Creates a new CancelActionNotAllowed with
     * {@link SpServerError#SP_ACTION_NOT_CANCELABLE} error.
     */
    public CancelActionNotAllowedException() {
        super(SpServerError.SP_ACTION_NOT_CANCELABLE);
    }

    /**
     * @param cause
     *            for the exception
     */
    public CancelActionNotAllowedException(final Throwable cause) {
        super(SpServerError.SP_ACTION_NOT_CANCELABLE, cause);
    }

    /**
     * @param message
     *            of the error
     */
    public CancelActionNotAllowedException(final String message) {
        super(message, SpServerError.SP_ACTION_NOT_CANCELABLE);
    }
}
