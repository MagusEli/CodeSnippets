package eli.engine.scene.geom.processor;

import eli.engine.scene.geom.MeshData;
import eli.engine.utils.Utils;
import org.joml.Vector3f;
import org.joml.Vector3i;

import java.util.ArrayList;
import java.util.List;

/** This class was made with help from https://icospheric.com/blog/2016/07/17/making-terraced-terrain/ **/
public class SphericalTerraceProcessor implements MeshProcessor {

    private float terraceHeight;

    public SphericalTerraceProcessor(float terraceHeight) {
        this.terraceHeight = terraceHeight;
    }

    public MeshData process(MeshData data) {

        List<Vector3f> vertices = data.getVertexPositions();
        List<Integer> colors = data.getVertexColors();
        List<Vector3i> triangles = data.getTriangles();

        int ndf = 0;
        int n3a = 0;
        int n2a = 0;
        int n1a = 0;

        List<Vector3f> nVerts = new ArrayList<>();
        List<Vector3f> nNorms = new ArrayList<>();
        List<Integer> nCols = new ArrayList<>();
        List<Vector3i> nTris = new ArrayList<>();

        for (Vector3i tri : triangles) {

            Vector3f a = vertices.get(tri.x);
            Vector3f b = vertices.get(tri.y);
            Vector3f c = vertices.get(tri.z);

            Vector3f v1 = a;
            Vector3f v2 = b;
            Vector3f v3 = c;

            float h1 = v1.length();
            float h2 = v2.length();
            float h3 = v3.length();

            float minHeight = Math.min(h1, Math.min(h2, h3));
            float maxHeight = Math.max(h1, Math.max(h2, h3));

            int minSlice = (int) (minHeight / terraceHeight);
            int maxSlice = (int) (maxHeight / terraceHeight);

            int acol = colors.get(tri.x);
            int bcol = colors.get(tri.y);
            int ccol = colors.get(tri.z);

            for (int i = minSlice; i <= maxSlice; i++) {

                float h = i * terraceHeight;
                int above = 0;

                if (h1 < h) {
                    if (h2 < h) {
                        if (h3 < h) {
                            // Shouldn't happen
                            System.out.println("Weirdness happened, 3 points below!");
                        }
                        else {
                            above = 1;
                        }
                    }
                    else {
                        if (h3 < h) {
                            above = 1;
                            v1 = c;
                            v2 = a;
                            v3 = b;
                        }
                        else {
                            above = 2;
                            v1 = b;
                            v2 = c;
                            v3 = a;
                        }
                    }
                }
                else {
                    if (h2 < h) {
                        if (h3 < h) {
                            above = 1;
                            v1 = b;
                            v2 = c;
                            v3 = a;
                        }
                        else {
                            above = 2;
                            v1 = c;
                            v2 = a;
                            v3 = b;
                        }
                    }
                    else {
                        if (h3 < h) {
                            above = 2;
                        }
                        else {
                            above = 3;
                        }
                    }
                }


                h1 = v1.length();
                h2 = v2.length();
                h3 = v3.length();

                // Current height plane
                Vector3f v1c = new Vector3f(v1).normalize().mul(h);
                Vector3f v2c = new Vector3f(v2).normalize().mul(h);
                Vector3f v3c = new Vector3f(v3).normalize().mul(h);

                int iv = nVerts.size();

                if (above == 3) {
                    n3a++;
                    nVerts.add(v1c);
                    nVerts.add(v2c);
                    nVerts.add(v3c);
                    nNorms.add(new Vector3f(v1c).normalize());
                    nNorms.add(new Vector3f(v2c).normalize());
                    nNorms.add(new Vector3f(v3c).normalize());
                    nCols.add(acol);
                    nCols.add(bcol);
                    nCols.add(ccol);
                    nTris.add(new Vector3i(iv, iv + 1, iv + 2));
                }

                else {

                    float t1 = (h1 - h) / (h1 - h3); // interpolation for v1
                    if (t1 < 0f) t1 = 0f; if (t1 > 1f) t1 = 1f;
                    Vector3f v1cn = new Vector3f();
                    v1c.lerp(v3c, t1, v1cn);

                    Vector3f v1bn = new Vector3f(v1cn).normalize().mul(v1cn.length() - terraceHeight);

                    float t2 = (h2 - h) / (h2 - h3);
                    if (t2 < 0f) t2 = 0f; if (t2 > 1f) t2 = 1f;

                    Vector3f v2cn = new Vector3f();
                    v2c.lerp(v3c, t2, v2cn);
                    Vector3f v2bn = new Vector3f(v2cn).normalize().mul(v2cn.length() - terraceHeight);

                    if (above == 2) {
                        n2a++;
                        // roof
                        nVerts.add(v1c);
                        nVerts.add(v2c);
                        nVerts.add(v2cn);
                        nVerts.add(v1cn);
                        nNorms.add(new Vector3f(v1c).normalize());
                        nNorms.add(new Vector3f(v2c).normalize());
                        nNorms.add(new Vector3f(v2cn).normalize());
                        nNorms.add(new Vector3f(v1cn).normalize());
                        nCols.add(acol);
                        nCols.add(bcol);
                        nCols.add(ccol);
                        nCols.add(acol);
                        nTris.add(new Vector3i(iv, iv + 1, iv + 2));
                        nTris.add(new Vector3i(iv + 2, iv + 3, iv));
                        iv += 4;

                        // walls
                        nVerts.add(v1cn);
                        nVerts.add(v2cn);
                        nVerts.add(v2bn);
                        nVerts.add(v1bn);

                        // normals calc
                        Vector3f ab = new Vector3f(v1cn).sub(v2cn);
                        Vector3f bc = new Vector3f(v2cn).sub(v2bn);
                        ab.cross(bc);
                        nNorms.add(ab);
                        nNorms.add(ab);
                        nNorms.add(ab);
                        nNorms.add(ab);

                        nCols.add(acol);
                        nCols.add(bcol);
                        nCols.add(ccol);
                        nCols.add(acol);
                        nTris.add(new Vector3i(iv, iv + 1, iv + 2));
                        nTris.add(new Vector3i(iv, iv + 2, iv + 3));
                    }
                    else if (above == 1) {
                        n1a++;
                        // roof
                        nVerts.add(v3c);
                        nVerts.add(v1cn);
                        nVerts.add(v2cn);
                        nNorms.add(new Vector3f(v3c).normalize());
                        nNorms.add(new Vector3f(v1cn).normalize());
                        nNorms.add(new Vector3f(v2cn).normalize());

                        nCols.add(acol);
                        nCols.add(bcol);
                        nCols.add(ccol);
                        nTris.add(new Vector3i(iv, iv + 1, iv + 2));
                        iv += 3;

                        // walls
                        nVerts.add(v2cn);
                        nVerts.add(v1cn);
                        nVerts.add(v1bn);
                        nVerts.add(v2bn);

                        Vector3f ab = new Vector3f(v2cn).sub(v1cn);
                        Vector3f bc = new Vector3f(v1cn).sub(v1bn);
                        ab.cross(bc);
                        nNorms.add(ab);
                        nNorms.add(ab);
                        nNorms.add(ab);
                        nNorms.add(ab);

                        nCols.add(acol);
                        nCols.add(bcol);
                        nCols.add(ccol);
                        nCols.add(acol);
                        nTris.add(new Vector3i(iv, iv + 1, iv + 3));
                        nTris.add(new Vector3i(iv + 1, iv + 2, iv + 3));
                    }
                }
            }
        }

        data.setVertexPositions(nVerts);
        data.setVertexNormals(nNorms);
        data.setVertexColors(nCols);
        data.setTriangles(nTris);

        System.out.println("totals ->  defaulted: " + ndf + ",  3 above: " + n3a + ",  2 above: " + n2a + ",  1 above: " + n1a);
        System.out.println("vertices: " + nVerts.size() + ",  normals: " + nNorms.size() + ",  colors: " + nCols.size() + ",  tris: " + nTris.size());

        return data;
    }
}
