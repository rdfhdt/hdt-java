package org.rdfhdt.hdt.iterator.utils;

import java.io.Closeable;
import java.util.Iterator;
import java.util.NoSuchElementException;

public abstract class RepeatApplyIterator<T> implements Iterator<T>, Closeable
{
    private Iterator<T> input ;
    private boolean finished = false ;
    private Iterator<T> currentStage = null ;

    protected RepeatApplyIterator(Iterator<T> input)
    {
        this.input = input ;
    }

    @Override
    public boolean hasNext()
    {
        if  ( finished )
            return false ;
        for ( ;; )
        {
            if ( currentStage == null && input.hasNext() )
            {
                T nextItem = input.next();
                currentStage = makeNextStage(nextItem) ;
            }
            
            if ( currentStage == null  )
            {
                finished = true ;
                return false ;
            }
            
            if ( currentStage.hasNext() )
                return true ;
            
            currentStage = null ;
        }
    }

    protected abstract Iterator<T> makeNextStage(T t) ;
    
    @Override
    public T next()
    {
        if ( ! hasNext() )
            throw new NoSuchElementException(this.getClass().getName()+".next()/finished") ;
        return currentStage.next() ;
    }

    @Override
    public final void remove()
    { throw new UnsupportedOperationException() ; }
    
    @Override
    public void close()
    {
    }
}