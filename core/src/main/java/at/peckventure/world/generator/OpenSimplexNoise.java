package at.peckventure.world.generator;

/**
 * OpenSimplexNoise – 2D-Implementierung.
 *
 * Diese Klasse basiert auf einer Public-Domain-Implementierung und liefert
 * „schöneres“ Noise als herkömmlicher Value‑Noise.
 */
public class OpenSimplexNoise {

    private static final double STRETCH_CONSTANT_2D = -0.211324865405187;   // (1/Math.sqrt(2+1)-1)/2
    private static final double SQUISH_CONSTANT_2D  =  0.366025403784439;   // (Math.sqrt(2+1)-1)/2
    private static final double NORM_CONSTANT_2D    = 47.0;

    private short[] perm;

    /**
     * Konstruktor, der den Noise-Generator mit einem Seed initialisiert.
     *
     * @param seed Der Seed für den Zufallsgenerator.
     */
    public OpenSimplexNoise(long seed) {
        perm = new short[256];
        short[] source = new short[256];
        for (short i = 0; i < 256; i++) {
            source[i] = i;
        }
        for (int i = 255; i >= 0; i--) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int r = (int)((seed + 31) % (i + 1));
            if (r < 0)
                r += (i + 1);
            perm[i] = source[r];
            source[r] = source[i];
        }
    }

    /**
     * Evaluierung des 2D-OpenSimplex Noise an der Stelle (x, y).
     *
     * @param x X-Koordinate.
     * @param y Y-Koordinate.
     * @return Noise-Wert im Bereich ca. [-1, 1].
     */
    public double eval(double x, double y) {
        // 1. Place input coordinates onto grid.
        double stretchOffset = (x + y) * STRETCH_CONSTANT_2D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;

        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);

        double squishOffset = (xsb + ysb) * SQUISH_CONSTANT_2D;
        double xb = xsb + squishOffset;
        double yb = ysb + squishOffset;

        double xins = xs - xsb;
        double yins = ys - ysb;
        double inSum = xins + yins;

        // 2. Compute contributions (3 corners)
        double dx0 = x - xb;
        double dy0 = y - yb;
        double value = 0;

        // Contribution (0,0)
        double attn0 = 2 - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0) {
            attn0 *= attn0;
            value += attn0 * attn0 * extrapolate(xsb, ysb, dx0, dy0);
        }

        // Determine which simplex (triangle) we are in.
        int xsb1, ysb1;
        double dx1, dy1;
        if (xins > yins) {
            xsb1 = xsb + 1;
            ysb1 = ysb;
            dx1 = dx0 - 1 - SQUISH_CONSTANT_2D;
            dy1 = dy0 - 0 - SQUISH_CONSTANT_2D;
        } else {
            xsb1 = xsb;
            ysb1 = ysb + 1;
            dx1 = dx0 - 0 - SQUISH_CONSTANT_2D;
            dy1 = dy0 - 1 - SQUISH_CONSTANT_2D;
        }
        double attn1 = 2 - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0) {
            attn1 *= attn1;
            value += attn1 * attn1 * extrapolate(xsb1, ysb1, dx1, dy1);
        }

        // Contribution (1,1)
        double dx2 = dx0 - 1 - 2 * SQUISH_CONSTANT_2D;
        double dy2 = dy0 - 1 - 2 * SQUISH_CONSTANT_2D;
        double attn2 = 2 - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0) {
            attn2 *= attn2;
            value += attn2 * attn2 * extrapolate(xsb + 1, ysb + 1, dx2, dy2);
        }

        return value / NORM_CONSTANT_2D;
    }

    // Hilfsmethode für den Boden (Floor) – schnell
    private int fastFloor(double x) {
        int xi = (int) x;
        return x < xi ? xi - 1 : xi;
    }

    /**
     * Ermittelt anhand des Permutationsarrays einen Gradienten und berechnet das Skalarprodukt.
     */
    private double extrapolate(int xsb, int ysb, double dx, double dy) {
        int index = perm[(perm[xsb & 0xFF] + ysb) & 0xFF] & 0x0E;
        double gx = gradients2D[index];
        double gy = gradients2D[index + 1];
        return gx * dx + gy * dy;
    }

    // 2D-Gradienten (4 Richtungen, jeweils positiv und negativ) – für gute Verteilung
    private static final double[] gradients2D = {
        5,  2,    2,  5,
        -5,  2,   -2,  5,
        5, -2,    2, -5,
        -5, -2,   -2, -5,
    };
}

