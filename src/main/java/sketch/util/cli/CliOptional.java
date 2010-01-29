package sketch.util.cli;

import static sketch.util.DebugOut.assertFalse;

/**
 * wrapper for command line options with a field designating whether they were set or not.
 * unlike Scala's option though, the value is always set (a default must be provided).
 * 
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you make
 *          changes, please consider contributing back!
 */
public class CliOptional<T> {
    public T value;
    public Class<T> valueClass;
    public boolean isSet;

    @SuppressWarnings("unchecked")
    public CliOptional(T defaultValue) {
        value = defaultValue;
        valueClass = (Class<T>) defaultValue.getClass();
    }

    public void setValue(Object value) {
        if (!valueClass.isAssignableFrom(value.getClass())) {
            assertFalse("given incompatible type", value.getClass(),
                    "for option of type", valueClass);
        }
        this.value = valueClass.cast(value);
        this.isSet = true;
    }
}
