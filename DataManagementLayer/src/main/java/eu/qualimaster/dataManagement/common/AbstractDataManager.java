package eu.qualimaster.dataManagement.common;

import java.lang.reflect.Modifier;
import java.util.*;

import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;

import eu.qualimaster.dataManagement.events.ReferenceDataManagementEvent;
import eu.qualimaster.dataManagement.events.ReferenceDataManagementEvent.Command;
import eu.qualimaster.dataManagement.sinks.IDataSink;
import eu.qualimaster.dataManagement.sources.IDataSource;
import eu.qualimaster.dataManagement.strategies.IStorageStrategyDescriptor;

/**
 * Implements then management of a data element in an abstract reusable way. Depending on the implementation
 * of {@link #createFallback(Class)}, no factory may be required to obtain data element instances. Actually, 
 * we assume that a data manager either handles a {@link IDataSource data source}, {@link IDataSink data sinks} or 
 * {@link IDataElement data elements}.
 * 
 * @param <E> the specific type of data element being managed
 * @author Holger Eichelberger
 */
public abstract class AbstractDataManager <E extends IDataElement> {

    private static final Logger LOGGER = LogManager.getLogger(AbstractDataManager.class);
    private List<IDataElementFactory<E>> factories 
        = Collections.synchronizedList(new ArrayList<IDataElementFactory<E>>());

    private Map<String, List<IReference<E>>> units = new HashMap<String, List<IReference<E>>>();
    private Class<E> elementClass;
    private String id;
    
    /**
     * Creates a new data manager and passes the class of elements to be handled.
     * 
     * @param elementClass the element class
     * @param id the unique identifier for this manager within a JVM, shall be the same across JVMs
     */
    protected AbstractDataManager(Class<E> elementClass, String id) {
        this.elementClass = elementClass;
        this.id = id;
    }

    /**
     * Returns the element class handled by this manager.
     * 
     * @return the element class
     */
    public Class<E> getElementClass() {
        return elementClass;
    }
    
    /**
     * Returns whether this data manager manages a data source.
     * 
     * @return <code>true</code> if it manages a data source, <code>false</code> else
     */
    public boolean managesDataSource() {
        return IDataSource.class.isAssignableFrom(elementClass);
    }

    /**
     * Returns whether this data manager manages a data sink.
     * 
     * @return <code>true</code> if it manages a data sink, <code>false</code> else
     */
    public boolean managesDataSink() {
        return IDataSink.class.isAssignableFrom(elementClass);
    }

    /**
     * Registers a factory.
     * 
     * @param factory the factory to be registered
     */
    public void registerFactory(IDataElementFactory<E> factory) {
        if (null != factory && !factories.contains(factory)) {
            factories.add(factory);
        }
    }
    
    /**
     * Unregisters a factory.
     * 
     * @param factory the factory to be unregistered
     */
    public void unregisterFactory(IDataElementFactory<E> factory) {
        factories.remove(factory);
    }

    /**
     * Creates a fallback instance in case that no factory provides a data management instance.
     * 
     * @param <S> the actual type of data element to be created
     * @param cls the specific class of data element to be created
     * @return the created instance (may be <b>null</b>)
     */
    protected <S extends E> S createFallback(Class<S> cls) {
        S result = null;
        if (null != cls && !cls.isInterface() && !Modifier.isAbstract(cls.getModifiers())) {
            try {
                result = cls.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                LOGGER.error("creating fallback data management element", e);
            }
        }
        return result;
    }

    /**
     * Creates a data element for <code>cls</code> and assigns it to <code>unit</code>. Please note that multiple
     * different data sources for one <code>cls</code> may be created, but the underlying factory may also just
     * return individual proxies to the same physical data source (e.g., hidden behind a buffering mechanism).
     *
     * @param <S> the actual type of data element to be created
     * @param unit the (symbolic) name of a unit to assign the created instance to,
     *   no assignment will happen if <code>unit</code> is <b>null</b>, but then also {@link #connectAll(String)},
     *   {@link #disconnectAll(String)} and {@link #discardAll(String)} will be useless.
     * @param cls the class of the data element to be created
     * @param strategy the storage strategy descriptor
     * @return an instance of <code>cls</code> in the most easiest case, a modified/wrapped 
     *   instance of <code>cls</code> in case of transparent data storage, <b>null</b> if no instance
     *   can be created, e.g., in case that <code>cls</code> is an abstract class or an interface
     */
    protected <S extends E> S create(String unit, Class<S> cls, IStorageStrategyDescriptor strategy) {
        S result = null;

        // 2017-02-07 Tuan: Debug the creation of data source
        LOGGER.info("Factories: ");
        Iterator<IDataElementFactory<E>> iter = factories.iterator();
        while (iter.hasNext()) {
            LOGGER.info("DEF: " + iter.next().getClass().getName());
        }
        if (null != cls) {
            for (int c = 0; null == result && c < factories.size(); c++) {
                result = factories.get(c).create(cls);
            }
            if (null == result) {
                result = createFallback(cls);
            }
            if (null != result) {
                result.setStrategy(strategy);
            }
        }
        register(unit, new LocalReference<E>(result));
        return result;
    }

    /**
     * Registers a created data element. Public for testing. Handle with care.
     * 
     * @param unit the (symbolic) name of a unit to assign the created instance to,
     *   no assignment will happen if <code>unit</code> is <b>null</b>
     * @param dataElement the data element to be registered with <code>unit</code>
     */
    protected void register(String unit, IReference<E> dataElement) {
        if (null != dataElement && null != unit) {
            synchronized (units) {
                List<IReference<E>> regU = units.get(unit);
                if (null == regU) {
                    regU = new ArrayList<IReference<E>>();
                    units.put(unit, regU);
                }
                regU.add(dataElement);
            }
            if (dataElement instanceof LocalReference) {
                ((LocalReference<E>) dataElement).register(this, unit);
            }
        }
    }
                
    /**
     * Connects all data elements known for the given <code>unit</code>.
     * 
     * @param unit the unit (no operation if <b>null</b>)
     */
    public void connectAll(String unit) {
        if (null != unit) {
            List<IReference<E>> elements = units.get(unit);
            if (null != elements) {
                for (int e = 0; e < elements.size(); e++) {
                    elements.get(e).connect();
                }
            }
        }
    }

    /**
     * Disconnects all data elements known for the given <code>unit</code>.
     * 
     * @param unit the unit (no operation if <b>null</b>)
     */
    public void disconnectAll(String unit) {
        if (null != unit) {
            List<IReference<E>> elements = units.get(unit);
            if (null != elements) {
                for (int e = 0; e < elements.size(); e++) {
                    elements.get(e).disconnect();
                }
            }
        }
    }
    
    /**
     * Discards all known elements for the given <code>unit</code>. Without
     * creating further data elements, {@link #connectAll(String)} and {@link #disconnectAll(String)}
     * will not perform any further operation.
     * 
     * @param unit the unit (no operation if <b>null</b>) 
     */
    public void discardAll(String unit) {
        if (null != unit) {
            synchronized (units) {
                units.remove(unit);
            }
        }
    }
    
    /**
     * An unique identifier for this manager.
     *
     * @return the unique identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Finds an element based on its containing <code>unit</code> and its <code>elementId</code>.
     * 
     * @param unit the unit to search for
     * @param elementId the element id to search for
     * @return the found element / reference, <b>null</b> if not found
     */
    private IReference<E> findElement(String unit, String elementId) {
        IReference<E> result = null;
        if (null != unit && null != elementId) {
            synchronized (units) {
                List<IReference<E>> elements = units.get(unit);
                if (null != elements) {
                    for (int e = 0, n = elements.size(); null == result && e < n; e++) {
                        IReference<E> element = elements.get(e);
                        if (elementId.equals(element.getId())) {
                            result = element;
                        }
                    }
                }
            }
        }
        return result;
    }
    
    /**
     * Handles the given event.
     * 
     * @param event the event to be handled
     */
    public void handle(ReferenceDataManagementEvent event) {
        Command cmd = event.getCommand();
        String unit = event.getUnit();
        if (null != unit) {
            IReference<E> element = findElement(unit, event.getElementId());
            switch (cmd) {
            case CONNECT:
                if (null != element) {
                    element.connect();
                } else {
                    LOGGER.warn("DataElement unknown: " + event);
                }
                break;
            case DISCONNECT:
                if (null != element) {
                    element.disconnect();
                } else {
                    LOGGER.warn("DataElement unknown: " + event);
                }
                break;
            case REGISTER:
                if (null == element) {
                    register(unit, new RemoteReference<E>(event));
                } else {
                    LOGGER.warn("DataElement already registered: " + event + ". - Ignore in local cluster.");
                }
                break;
            case DISPOSE:
                if (null != element) {
                    synchronized (units) {
                        units.remove(element);
                    }
                    element.dispose();
                } else {
                    LOGGER.warn("DataElement unknown: " + event);
                }
                break;
            default:
                LOGGER.warn("Unexpected command: " + cmd);
                break;
            }
        }
    }

}
