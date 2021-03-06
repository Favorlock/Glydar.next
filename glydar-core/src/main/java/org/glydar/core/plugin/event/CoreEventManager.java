package org.glydar.core.plugin.event;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashMap;
import java.util.Map;

import org.glydar.api.Backend;
import org.glydar.api.logging.GlydarLogger;
import org.glydar.api.plugin.Plugin;
import org.glydar.api.plugin.event.Event;
import org.glydar.api.plugin.event.EventExecutor;
import org.glydar.api.plugin.event.EventHandler;
import org.glydar.api.plugin.event.EventManager;
import org.glydar.api.plugin.event.EventPriority;
import org.glydar.api.plugin.event.Listener;
import org.glydar.core.plugin.event.RegisteredHandlers.RegisteredHandler;

import com.google.common.base.Predicate;

public class CoreEventManager implements EventManager {

    private static final String LOGGER_PREFIX = "Event Manager";

    private final GlydarLogger logger;
    private final Map<Class<? extends Event>, RegisteredHandlers> map;
    private int handlerIndex;

    public CoreEventManager(Backend backend) {
        this.logger = backend.getLogger(getClass(), LOGGER_PREFIX);
        this.map = new HashMap<>();
        this.handlerIndex = 0;
    }

    @Override
    public boolean register(Plugin plugin, Listener listener) {
        for (Method method : listener.getClass().getDeclaredMethods()) {
            EventHandler annotation = method.getAnnotation(EventHandler.class);
            if (annotation == null) {
                continue;
            }

            if (Modifier.isStatic(method.getModifiers())) {
                logInvalidEventHandlerMethod(method, "is static");
                continue;
            }

            if (!Modifier.isPublic(method.getModifiers())) {
                logInvalidEventHandlerMethod(method, "is not public");
                continue;
            }

            Class<?>[] parameterTypes = method.getParameterTypes();
            if (parameterTypes.length != 1) {
                logInvalidEventHandlerMethod(method, "does not have one and only one parameter");
                continue;
            }

            if (!Event.class.isAssignableFrom(parameterTypes[0])) {
                logInvalidEventHandlerMethod(method, "is not of type `Event`");
                continue;
            }

            register(plugin, listener, method, parameterTypes[0].asSubclass(Event.class), annotation);
        }

        return true;
    }

    private void logInvalidEventHandlerMethod(Method method, String what) {
        logger.warning("Event Handler Method `{0}` {1}, skipping", method, what);
    }

    private <E extends Event> void register(Plugin plugin, Listener listener, Method method, Class<E> eventClass,
            EventHandler annotation) {
        MethodEventExecutor<E> executor = new MethodEventExecutor<E>(eventClass, listener, method, annotation);
        register(plugin, eventClass, executor, annotation.priority());
    }

    @Override
    public <E extends Event> void register(Plugin plugin, Class<E> eventClass, EventExecutor<E> executor,
            EventPriority priority) {
        if (Modifier.isAbstract(eventClass.getModifiers())) {
            throw new UnsupportedOperationException();
        }

        RegisteredHandler handler = new RegisteredHandler(plugin, handlerIndex++, priority, executor);
        RegisteredHandlers handlers = getHandlers(eventClass);
        handlers.addHandler(handler);
    }

    /*
     * Retrieves or creates the {@link RegisteredHandlers} for the given event
     * class. <p/> If the RegisteredHandlers does not already exist, this method
     * will create it, recursively calling itself to get or create the
     * RegisteredHandlers of the superclass.
     */
    private RegisteredHandlers getHandlers(Class<? extends Event> eventClass) {
        RegisteredHandlers handlers = map.get(eventClass);
        if (handlers == null) {
            // #asSubclass is safe because we know eventClass is not abstract
            // and so cannot be Event.
            Class<? extends Event> eventSuperClass = eventClass.getSuperclass().asSubclass(Event.class);
            if (!Modifier.isAbstract(eventSuperClass.getModifiers())) {
                RegisteredHandlers parentHandlers = getHandlers(eventSuperClass);
                handlers = new RegisteredHandlers(parentHandlers);
            }
            else {
                handlers = new RegisteredHandlers();
            }

            map.put(eventClass, handlers);
        }

        return handlers;
    }

    @Override
    public void unregister(final EventExecutor<?> executor) {
        unregisterAllIf(new Predicate<RegisteredHandler>() {

            @Override
            public boolean apply(RegisteredHandler handler) {
                return handler.getExecutor() == executor;
            }
        });
    }

    @Override
    public void unregister(final Listener listener) {
        unregisterAllIf(new Predicate<RegisteredHandler>() {

            @Override
            public boolean apply(RegisteredHandler handler) {
                if (handler.getExecutor() instanceof MethodEventExecutor<?>) {
                    MethodEventExecutor<?> executor = (MethodEventExecutor<?>) handler.getExecutor();
                    return executor.getListener() == listener;
                }

                return false;
            }
        });
    }

    @Override
    public void unregisterAll(final Plugin plugin) {
        unregisterAllIf(new Predicate<RegisteredHandler>() {

            @Override
            public boolean apply(RegisteredHandler handler) {
                return handler.getPlugin() == plugin;
            }
        });
    }

    private void unregisterAllIf(Predicate<RegisteredHandler> predicate) {
        for (RegisteredHandlers handlers : map.values()) {
            handlers.removeHandlersIf(predicate);
        }
    }

    @Override
    public <E extends Event> E callEvent(E event) {
        RegisteredHandlers handlers = getHandlers(event.getClass());
        for (EventExecutor<?> rawExecutor : handlers.resolvedExecutors) {
            @SuppressWarnings("unchecked")
            EventExecutor<? super E> executor = (EventExecutor<? super E>) rawExecutor;
            try {
                executor.execute(event);
            }
            catch (Exception exc) {
                logger.warning(exc, "Exception thrown in Event handler");
            }
        }

        return event;
    }
}
