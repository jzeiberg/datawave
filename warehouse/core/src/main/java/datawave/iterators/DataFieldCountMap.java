package datawave.iterators;

import datawave.data.MetadataCardinalityCounts;
import org.apache.accumulo.core.data.Key;
import org.apache.accumulo.core.data.Value;
import org.apache.accumulo.core.iterators.Combiner;
import datawave.data.ColumnFamilyConstants;
import datawave.data.type.Type;
import org.apache.log4j.Logger;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;

public class DataFieldCountMap extends Combiner implements Serializable {
    
    private static final Logger log = Logger.getLogger(DataFieldCountMap.class);
    
    private ArrayList<Integer> ingestCountArray = new ArrayList<>();
    private byte[] startKey = null;
    
    /**
     * Reduces a list of Values into a single Value.
     *
     * @param key
     *            The most recent version of the Key being reduced.
     *
     * @param iterator
     *            An iterator over the Values for different versions of the key.
     *
     * @return The combined Value.
     */
    @Override
    public Value reduce(Key key, Iterator<Value> iterator) {
        
        StringBuilder newValue = new StringBuilder();
        
        while (iterator.hasNext()) {
            Value value = iterator.next();
            try {
                if (value.get() != null)
                    newValue.append(value.get()).append(";");
            } catch (Exception e) {
                log.error("Unable to decode counts from " + key + " / " + value);
            }
        }
        Value accumulator = new Value(newValue.toString().getBytes());
        return accumulator;
    }
    
}
