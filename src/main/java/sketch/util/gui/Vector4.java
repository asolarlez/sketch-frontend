package sketch.util.gui;

/**
 * vector of four floating point values
 * @author gatoatigrado (nicholas tung) [email: ntung at ntung]
 * @license This file is licensed under BSD license, available at
 *          http://creativecommons.org/licenses/BSD/. While not required, if you
 *          make changes, please consider contributing back!
 */
public class Vector4 {
    public float x, y, z, w;

    public Vector4(float x, float y, float z, float w) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.w = w;
    }

    public Vector4 scalar_multiply(float s) {
        return new Vector4(s * x, s * y, s * z, s * w);
    }

    public Vector4 add(Vector4 other) {
        return new Vector4(x + other.x, y + other.y, z + other.z, w + other.w);
    }

    public String hexColor() {
        return hexColorValue(x) + hexColorValue(y) + hexColorValue(z);
    }

    private String hexColorValue(float v) {
        int iv = (int) (255 * v);
        if (iv <= 0) {
            return "00";
        } else if (iv < 16) {
            return "0" + Integer.toHexString(iv);
        } else if (iv <= 255) {
            return Integer.toHexString(iv);
        } else {
            return "ff";
        }
    }
}
