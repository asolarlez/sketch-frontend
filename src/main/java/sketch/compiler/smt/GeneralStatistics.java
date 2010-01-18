package sketch.compiler.smt;

import java.util.HashMap;
import java.util.Map;

public class GeneralStatistics {
    
    Map<String, Object> mStore;
    
    public GeneralStatistics() {
        mStore = new HashMap<String, Object>();
    }
    
    public long getLong(String key) {
        if (mStore.containsKey(key))
            return (Long) mStore.get(key);
        else
            return 0;
    }
    
    public void put(String key, Object value) {
        mStore.put(key, value);
    }
    
    public long incrementLong(String key, long value) {
        long oldValue = 0;
        
        oldValue = getLong(key);
        
        oldValue += value;
        mStore.put(key, oldValue);
        return oldValue;
    }
    
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (String key : mStore.keySet()) {
            sb.append(key);
            sb.append("\t");
            sb.append(mStore.get(key));
            sb.append("\n");
        }
        
        return sb.toString();
    }
    
}
