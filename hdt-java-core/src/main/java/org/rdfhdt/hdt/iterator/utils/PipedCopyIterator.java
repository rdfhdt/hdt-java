package org.rdfhdt.hdt.iterator.utils;

import java.io.Closeable;
import java.io.IOException;
import java.util.Iterator;
import java.util.Objects;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Function;

/**
 * a utility class to create an iterator from the value returned by another Thread
 *
 * @param <T> the iterator type
 * @author Antoine Willerval
 */

public class PipedCopyIterator<T> implements Iterator<T>, Closeable {


    /**
     * RuntimeException generated by the PipedCopyIterator
     *
     * @author Antoine Willerval
     */
    public static class PipedIteratorException extends RuntimeException {
        public PipedIteratorException(String message, Throwable t) {
            super(message, t);
        }
    }


    /**
     * Callback for the {@link #createOfCallback(PipedCopyIterator.PipeCallBack)}  method
     *
     * @param <T> the iterator type
     * @author Antoine Willerval
     */
    @FunctionalInterface
    public interface PipeCallBack<T> {
        /**
         * method called from the new thread to generate the new data, at the end of the callback, the pipe is closed
         * with or without exception
         *
         * @param pipe the pipe to fill
         * @throws Exception any exception returned by the generator
         */
        void createPipe(PipedCopyIterator<T> pipe) throws Exception;
    }

    /**
     * create a piped iterator from a callback runner, the call to the callback should be made in the callbackRunner
     *
     * @param callbackRunner the callback runner
     * @param <T>            type of the iterator
     * @return the iterator
     */
    public static <T> PipedCopyIterator<T> createOfCallback(PipeCallBack<T> callbackRunner) {
        PipedCopyIterator<T> pipe = new PipedCopyIterator<>();

        Thread thread = new Thread(() -> {
            try {
                callbackRunner.createPipe(pipe);
                pipe.closePipe();
            } catch (Throwable e) {
                pipe.closePipe(e);
            }
        }, "PipeIterator");
        thread.start();

		// close the thread at end
		pipe.attachThread(thread);

        return pipe;
    }

    private interface QueueObject<T> {
        boolean end();

        T get();
    }

    private class ElementQueueObject implements QueueObject<T> {
        private final T obj;

        private ElementQueueObject(T obj) {
            this.obj = obj;
        }


        @Override
        public boolean end() {
            return false;
        }

        @Override
        public T get() {
            return obj;
        }
    }

    private class EndQueueObject implements QueueObject<T> {
        @Override
        public boolean end() {
            return true;
        }

        @Override
        public T get() {
            throw new IllegalArgumentException();
        }
    }

    private final ArrayBlockingQueue<QueueObject<T>> queue = new ArrayBlockingQueue<>(16);

    private T next;
    private boolean end;
    private PipedIteratorException exception;

    private Thread thread;

    @Override
    public boolean hasNext() {
        if (end) {
            return false;
        }
        if (next != null) {
            return true;
        }

        QueueObject<T> obj;
        try {
            obj = queue.take();
        } catch (InterruptedException e) {
            throw new PipedIteratorException("Can't read pipe", e);
        }

        if (obj.end()) {
            end = true;
            if (exception != null) {
                throw exception;
            }
            return false;
        }
        next = obj.get();
        return true;
    }

    @Override
    public T next() {
        if (!hasNext()) {
            return null;
        }
        T next = this.next;
        this.next = null;
        return next;
    }

    public void closePipe() {
        closePipe(null);
    }

    public void closePipe(Throwable e) {
        if (e != null) {
            if (e instanceof PipedIteratorException) {
                this.exception = (PipedIteratorException) e;
            } else {
                this.exception = new PipedIteratorException("closing exception", e);
            }
        }
        try {
            queue.put(new EndQueueObject());
        } catch (InterruptedException ee) {
            throw new PipedIteratorException("Can't close pipe", ee);
        }
    }

    /**
     * map this iterator to another type
     *
     * @param mappingFunction the mapping function
     * @param <E>             the future type
     * @return mapped iterator
     */
    public <E> Iterator<E> map(Function<T, E> mappingFunction) {
        return new MapIterator<>(this, mappingFunction);
    }

    /**
     * map this iterator to another type
     *
     * @param mappingFunction the mapping function
     * @param <E>             the future type
     * @return mapped iterator
     */
    public <E> Iterator<E> mapWithId(MapIterator.MapWithIdFunction<T, E> mappingFunction) {
        return new MapIterator<>(this, mappingFunction);
    }

    public void addElement(T node) {
        try {
            queue.put(new ElementQueueObject(node));
        } catch (InterruptedException ee) {
            throw new PipedIteratorException("Can't add element to pipe", ee);
        }
    }

    /**
     * attach a thread to interrupt with this iterator
     *
     * @param thread the thread
     */
    public void attachThread(Thread thread) {
        Objects.requireNonNull(thread, "thread can't be null!");
        if (this.thread != null && this.thread != thread) {
            throw new IllegalArgumentException("Thread already attached");
        }
        this.thread = thread;
    }

    @Override
    public void close() throws IOException {
		if (thread != null) {
			thread.interrupt();
		}
    }
}
