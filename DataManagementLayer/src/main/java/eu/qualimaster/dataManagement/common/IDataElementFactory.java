package eu.qualimaster.dataManagement.common;

/**
 * A factory for data elements. The aim of this factory is to enable
 * transparent storage for data elements, i.e., a data element just focuses
 * on the data to be produced, while the data element factory may wrap, instrument,
 * inject or proxy the original data element instance in order to transparently 
 * realize the most appropriate transparent data storage mechanism. In the extreme
 * case, a data element factory may even separate the real source from a virtual source
 * and return the virtual virtual source which obtains data items from the real source
 * through a buffer, e.g., a distributed message mechanism. This helps keeping 
 * generated topologies independent from the underlying data management mechanisms.
 * 
 * @param <E> the type of the data element being created
 * @author Holger Eichelberger
 */
public interface IDataElementFactory <E extends IDataElement> {

    /**
     * Creates a data element for class <code>cls</code>.
     * 
     * @param <S> the actual type to be created
     * @param cls the class of data element to create for
     * @return the created data element
     */
    public <S extends E> S create(Class<S> cls);

}
