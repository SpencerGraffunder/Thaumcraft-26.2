package thaumcraft.common.lib.crafting;

import org.junit.Test;
import static org.junit.Assert.*;
import thaumcraft.api.crafting.Part;

/**
 * Matrix 90-degree clockwise rotation tests (used to rotate multiblock blueprints).
 */
public class MatrixTest {
    private static Part p(Object tag) {
        return new Part(tag, null, false);
    }

    private static String cell(Part part) {
        return part == null ? "." : String.valueOf(part.getSource());
    }

    @Test
    public void zero_rotation_is_identity() {
        Part[][] m = { {p("a"), p("b")}, {p("c"), p("d")} };
        Matrix mat = new Matrix(m);
        mat.Rotate90DegRight(0);
        assertEquals(2, mat.getRows());
        assertEquals(2, mat.getCols());
        assertEquals("a", cell(mat.getMatrix()[0][0]));
        assertEquals("d", cell(mat.getMatrix()[1][1]));
    }

    @Test
    public void one_rotation_is_90_clockwise() {
        // 2 rows x 3 cols
        Part[][] m = { {p("a"), p("b"), p("c")}, {p("d"), p("e"), p("f")} };
        Matrix mat = new Matrix(m);
        mat.Rotate90DegRight(1);
        assertEquals(3, mat.getRows());
        assertEquals(2, mat.getCols());
        assertEquals("d", cell(mat.getMatrix()[0][0]));
        assertEquals("a", cell(mat.getMatrix()[0][1]));
        assertEquals("e", cell(mat.getMatrix()[1][0]));
        assertEquals("b", cell(mat.getMatrix()[1][1]));
        assertEquals("f", cell(mat.getMatrix()[2][0]));
        assertEquals("c", cell(mat.getMatrix()[2][1]));
    }

    @Test
    public void two_rotations_is_180() {
        Part[][] m = { {p("a"), p("b"), p("c")}, {p("d"), p("e"), p("f")} };
        Matrix mat = new Matrix(m);
        mat.Rotate90DegRight(2);
        assertEquals(2, mat.getRows());
        assertEquals(3, mat.getCols());
        assertEquals("f", cell(mat.getMatrix()[0][0]));
        assertEquals("e", cell(mat.getMatrix()[0][1]));
        assertEquals("d", cell(mat.getMatrix()[0][2]));
        assertEquals("c", cell(mat.getMatrix()[1][0]));
        assertEquals("b", cell(mat.getMatrix()[1][1]));
        assertEquals("a", cell(mat.getMatrix()[1][2]));
    }

    @Test
    public void three_rotations_is_270() {
        Part[][] m = { {p("a"), p("b"), p("c")}, {p("d"), p("e"), p("f")} };
        Matrix mat = new Matrix(m);
        mat.Rotate90DegRight(3);
        assertEquals(3, mat.getRows());
        assertEquals(2, mat.getCols());
        // 270 clockwise = 90 counter-clockwise:
        //   f e d
        //   c b a  (as 3x2: col0 = f,c ; col1 = e,b ; wait compute)
        assertEquals("c", cell(mat.getMatrix()[0][0]));
        assertEquals("f", cell(mat.getMatrix()[0][1]));
        assertEquals("b", cell(mat.getMatrix()[1][0]));
        assertEquals("e", cell(mat.getMatrix()[1][1]));
        assertEquals("a", cell(mat.getMatrix()[2][0]));
        assertEquals("d", cell(mat.getMatrix()[2][1]));
    }

    @Test
    public void four_rotations_is_identity() {
        Part[][] m = { {p("a"), p("b"), p("c")}, {p("d"), p("e"), p("f")} };
        Matrix mat = new Matrix(m);
        mat.Rotate90DegRight(4);
        assertEquals(2, mat.getRows());
        assertEquals(3, mat.getCols());
        assertEquals("a", cell(mat.getMatrix()[0][0]));
        assertEquals("c", cell(mat.getMatrix()[0][2]));
        assertEquals("d", cell(mat.getMatrix()[1][0]));
        assertEquals("f", cell(mat.getMatrix()[1][2]));
    }

    @Test
    public void square_matrix_rotation() {
        Part[][] m = { {p("1"), p("2")}, {p("3"), p("4")} };
        Matrix mat = new Matrix(m);
        mat.Rotate90DegRight(1);
        assertEquals(2, mat.getRows());
        assertEquals(2, mat.getCols());
        // 90 clockwise of [[1,2],[3,4]] -> [[3,1],[4,2]]
        assertEquals("3", cell(mat.getMatrix()[0][0]));
        assertEquals("1", cell(mat.getMatrix()[0][1]));
        assertEquals("4", cell(mat.getMatrix()[1][0]));
        assertEquals("2", cell(mat.getMatrix()[1][1]));
    }
}
