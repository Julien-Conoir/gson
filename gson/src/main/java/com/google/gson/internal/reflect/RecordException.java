package com.google.gson.internal.reflect;

/**
 * This exception is raised if there is a ReflectiveOperationException or a IllegalAccessException throws.
 *
 * @author Conoir Julien
 */
public class RecordException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * Creates exception with the specified message and cause.
     *
     * @param msg error message describing what happened.
     * @param cause root exception that caused this exception to be thrown.
     */
    public RecordException(String msg, Throwable cause) { super(msg, cause); }
}
