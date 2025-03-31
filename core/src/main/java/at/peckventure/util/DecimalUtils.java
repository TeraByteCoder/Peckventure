package at.peckventure.util;

public class DecimalUtils
{
    public static float round(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return (float) (Math.round(value * factor) / factor);
    }

    public static float floor(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return (float) (Math.floor(value * factor) / factor);
    }

    public static float ceil(double value, int decimals) {
        double factor = Math.pow(10, decimals);
        return (float) (Math.ceil(value * factor) / factor);
    }
}
