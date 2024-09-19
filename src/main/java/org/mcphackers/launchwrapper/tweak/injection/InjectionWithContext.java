package org.mcphackers.launchwrapper.tweak.injection;

public abstract class InjectionWithContext<T> implements Injection {
    /**
     * Used to store processed data between injections
     */
    protected T context;
    
    /**
     * @param context Used to store processed data between injections
     */
    public InjectionWithContext(T context) {
        this.context = context;
    }
}
