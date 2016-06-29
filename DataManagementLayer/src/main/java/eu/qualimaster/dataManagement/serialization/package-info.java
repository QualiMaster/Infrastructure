/**
 * Implements a simple framework for the serialization of input/output data objects to the data management
 * layer and to hardware. In contrast to serialization frameworks such as kryo, this simple framework
 * allows extensible serializer streams (for hardware, for the data management layer) as well as declared metadata on 
 * the structure of the object allowing the data management layer to work more efficiently.
 * 
 * For using this serialization framework, implement for each class to be serialized an 
 * {@link eu.qualimaster.dataManagement.serialization.ISerializer} and register it with 
 * {@link eu.qualimaster.dataManagement.serialization.SerializerRegistry}. 
 * 
 * Basically, the serializers serve for two purposes, serializing from/to hardware via input/output streams 
 * (frontending Google protobuf) and via {@link IDataInput}/{@link IDataOutput} for internal purposes, e.g., to
 * use a data replay mechanism generically to test / profile pipeline parts. 
 */
package eu.qualimaster.dataManagement.serialization;