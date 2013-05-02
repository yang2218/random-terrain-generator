
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;

import javax.media.opengl.GL;
import javax.media.opengl.GLAutoDrawable;
import javax.media.opengl.GLCanvas;
import javax.media.opengl.GLEventListener;
import javax.media.opengl.glu.GLU;
import javax.swing.JFrame;
import javax.vecmath.Point3f;
import javax.vecmath.Vector3f;

import com.sun.opengl.util.BufferUtil;
import com.sun.opengl.util.FPSAnimator;

public class objReader extends JFrame implements MouseListener, MouseMotionListener, GLEventListener, KeyListener {

    class objModel {

        public FloatBuffer vertexBuffer;
        public IntBuffer faceBuffer;
        public FloatBuffer normalBuffer;
        public Point3f center;
        public int num_verts;		// number of vertices
        public int num_faces;		// number of triangle faces

        public void Draw() {
            vertexBuffer.rewind();
            normalBuffer.rewind();
            faceBuffer.rewind();
            gl.glEnableClientState(GL.GL_VERTEX_ARRAY);
            gl.glEnableClientState(GL.GL_NORMAL_ARRAY);

            gl.glVertexPointer(3, GL.GL_FLOAT, 0, vertexBuffer);
            gl.glNormalPointer(GL.GL_FLOAT, 0, normalBuffer);

            gl.glDrawElements(GL.GL_TRIANGLES, num_faces * 3, GL.GL_UNSIGNED_INT, faceBuffer);

            gl.glDisableClientState(GL.GL_VERTEX_ARRAY);
            gl.glDisableClientState(GL.GL_NORMAL_ARRAY);
        }

        public objModel(String filename) {
            /* load a triangular mesh model from a .obj file */
            BufferedReader in = null;
            try {
                in = new BufferedReader(new FileReader(filename));
            } catch (IOException e) {
                System.out.println("Error reading from file " + filename);
                System.exit(0);
            }

            center = new Point3f();
            float x, y, z;
            int v1, v2, v3;
            float minx, miny, minz;
            float maxx, maxy, maxz;
            float bbx, bby, bbz;
            minx = miny = minz = 10000.f;
            maxx = maxy = maxz = -10000.f;

            String line;
            String[] tokens;
            ArrayList<Point3f> input_verts = new ArrayList<Point3f>();
            ArrayList<Integer> input_faces = new ArrayList<Integer>();
            ArrayList<Vector3f> input_norms = new ArrayList<Vector3f>();
            try {
                while ((line = in.readLine()) != null) {
                    if (line.length() == 0) {
                        continue;
                    }
                    switch (line.charAt(0)) {
                        case 'v':
                            tokens = line.split("[ ]+");
                            x = Float.valueOf(tokens[1]);
                            y = Float.valueOf(tokens[2]);
                            z = Float.valueOf(tokens[3]);
                            minx = Math.min(minx, x);
                            miny = Math.min(miny, y);
                            minz = Math.min(minz, z);
                            maxx = Math.max(maxx, x);
                            maxy = Math.max(maxy, y);
                            maxz = Math.max(maxz, z);
                            input_verts.add(new Point3f(x, y, z));
                            center.add(new Point3f(x, y, z));
                            break;
                        case 'f':
                            tokens = line.split("[ ]+");
                            v1 = Integer.valueOf(tokens[1]) - 1;
                            v2 = Integer.valueOf(tokens[2]) - 1;
                            v3 = Integer.valueOf(tokens[3]) - 1;
                            input_faces.add(v1);
                            input_faces.add(v2);
                            input_faces.add(v3);
                            break;
                        default:
                            continue;
                    }
                }
                in.close();
            } catch (IOException e) {
                System.out.println("Unhandled error while reading input file.");
            }

            System.out.println("Read " + input_verts.size()
                    + " vertices and " + input_faces.size() + " faces.");

            center.scale(1.f / (float) input_verts.size());

            bbx = maxx - minx;
            bby = maxy - miny;
            bbz = maxz - minz;
            float bbmax = Math.max(bbx, Math.max(bby, bbz));

            for (Point3f p : input_verts) {

                p.x = (p.x - center.x) / bbmax;
                p.y = (p.y - center.y) / bbmax;
                p.z = (p.z - center.z) / bbmax;
            }
            center.x = center.y = center.z = 0.f;

            /* estimate per vertex average normal */
            int i;
            for (i = 0; i < input_verts.size(); i++) {
                input_norms.add(new Vector3f());
            }

            Vector3f e1 = new Vector3f();
            Vector3f e2 = new Vector3f();
            Vector3f tn = new Vector3f();
            for (i = 0; i < input_faces.size(); i += 3) {
                v1 = input_faces.get(i + 0);
                v2 = input_faces.get(i + 1);
                v3 = input_faces.get(i + 2);

                e1.sub(input_verts.get(v2), input_verts.get(v1));
                e2.sub(input_verts.get(v3), input_verts.get(v1));
                tn.cross(e1, e2);
                input_norms.get(v1).add(tn);

                e1.sub(input_verts.get(v3), input_verts.get(v2));
                e2.sub(input_verts.get(v1), input_verts.get(v2));
                tn.cross(e1, e2);
                input_norms.get(v2).add(tn);

                e1.sub(input_verts.get(v1), input_verts.get(v3));
                e2.sub(input_verts.get(v2), input_verts.get(v3));
                tn.cross(e1, e2);
                input_norms.get(v3).add(tn);
            }

            /* convert to buffers to improve display speed */
            for (i = 0; i < input_verts.size(); i++) {
                input_norms.get(i).normalize();
            }

            vertexBuffer = BufferUtil.newFloatBuffer(input_verts.size() * 3);
            normalBuffer = BufferUtil.newFloatBuffer(input_verts.size() * 3);
            faceBuffer = BufferUtil.newIntBuffer(input_faces.size());

            for (i = 0; i < input_verts.size(); i++) {
                vertexBuffer.put(input_verts.get(i).x);
                vertexBuffer.put(input_verts.get(i).y);
                vertexBuffer.put(input_verts.get(i).z);
                normalBuffer.put(input_norms.get(i).x);
                normalBuffer.put(input_norms.get(i).y);
                normalBuffer.put(input_norms.get(i).z);
            }

            for (i = 0; i < input_faces.size(); i++) {
                faceBuffer.put(input_faces.get(i));
            }
            num_verts = input_verts.size();
            num_faces = input_faces.size() / 3;
        }
    }
    //models to be selected from
    private objModel tree_aspen = new objModel("tree_aspen.obj");
    private objModel tree_conical = new objModel("tree_conical.obj");
    private objModel tulip = new objModel("tulip.obj");
    private objModel cactus = new objModel("cactus.obj");
    private objModel plant = new objModel("plant.obj");
    private objModel statue = new objModel("statue.obj");
    private ArrayList<objPosition> sceneObjects;
    //scene rendering
    private final GLCanvas canvas;
    private GL gl;
    private final GLU glu = new GLU();
    private FPSAnimator animator;
    private int winW = 800, winH = 800;
    private boolean wireframe = false;
    private boolean cullface = true;
    private boolean flatshade = false;
    private float xpos = 0, ypos = 0, zpos = 0;
    private float centerx, centery, centerz;
    private float roth = 0, rotv = 0;
    private float znear, zfar;
    private int mouseX, mouseY, mouseButton;
    private float motionSpeed, rotateSpeed;
    private float animation_speed = 1.0f;
    TerrainGenerator terrain;
    int terrainDim = 512;
    float terrainSharpness = 3f;
    float terrainSize = 30f;
    Vector3f[][] points;
    private boolean showTerrain = false;
    private float xmin = -4f, ymin = -4f, zmin = -4f;
    private float xmax = 4f, ymax = 4f, zmax = 4f;

    public objReader() {
        super("Procedural Terrain Gen");
        canvas = new GLCanvas();
        canvas.addGLEventListener(this);
        canvas.addKeyListener(this);
        canvas.addMouseListener(this);
        canvas.addMouseMotionListener(this);
        animator = new FPSAnimator(canvas, 30);	// create a 30 fps animator
        getContentPane().add(canvas);
        setSize(winW, winH);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setVisible(true);
        animator.start();
        canvas.requestFocus();
    }

    void initViewParameters() {
        roth = rotv = 0;

        float ball_r = (float) Math.sqrt((xmax - xmin) * (xmax - xmin)
                + (ymax - ymin) * (ymax - ymin)
                + (zmax - zmin) * (zmax - zmin)) * 0.707f;

        centerx = (xmax + xmin) / 2.f;
        centery = (ymax + ymin) / 2.f;
        centerz = (zmax + zmin) / 2.f;
        xpos = centerx;
        ypos = centery;
        zpos = ball_r / (float) Math.sin(45.f * Math.PI / 180.f) + centerz;

        znear = 0.01f;
        zfar = 1000.f;

        motionSpeed = 0.002f * ball_r;
        rotateSpeed = 0.1f;

    }

    public void reshape(GLAutoDrawable drawable, int x, int y, int width, int height) {
        winW = width;
        winH = height;

        gl.glViewport(0, 0, width, height);
        gl.glMatrixMode(GL.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluPerspective(45.f, (float) width / (float) height, znear, zfar);
        gl.glMatrixMode(GL.GL_MODELVIEW);
    }

    public void init(GLAutoDrawable drawable) {
        gl = drawable.getGL();

        initViewParameters();
        gl.glClearColor(.3f, .3f, .8f, 1f);
        gl.glClearDepth(1.0f);

        // white light at the eye
        float light0_position[] = {0, 0, 1, 0};
        float light0_diffuse[] = {1, 1, 1, 1};
        float light0_specular[] = {1, 1, 1, 1};
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_POSITION, light0_position, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_DIFFUSE, light0_diffuse, 0);
        gl.glLightfv(GL.GL_LIGHT0, GL.GL_SPECULAR, light0_specular, 0);

        //red light
        float light1_position[] = {-.1f, .1f, 0, 0};
        float light1_diffuse[] = {.75f, .05f, .05f, 1};
        float light1_specular[] = {.75f, .05f, .05f, 1};
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_POSITION, light1_position, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_DIFFUSE, light1_diffuse, 0);
        gl.glLightfv(GL.GL_LIGHT1, GL.GL_SPECULAR, light1_specular, 0);

        //blue light
        float light2_position[] = {.1f, .1f, 0, 0};
        float light2_diffuse[] = {.05f, .05f, .75f, 1};
        float light2_specular[] = {.05f, .05f, .75f, 1};
        gl.glLightfv(GL.GL_LIGHT2, GL.GL_POSITION, light2_position, 0);
        gl.glLightfv(GL.GL_LIGHT2, GL.GL_DIFFUSE, light2_diffuse, 0);
        gl.glLightfv(GL.GL_LIGHT2, GL.GL_SPECULAR, light2_specular, 0);

        //cyan light
        float light3_position[] = {-.1f, -.1f, 0, 0};
        float light3_diffuse[] = {.05f, .8f, .75f, 1};
        float light3_specular[] = {.05f, .8f, .75f, 1};
        gl.glLightfv(GL.GL_LIGHT3, GL.GL_POSITION, light3_position, 0);
        gl.glLightfv(GL.GL_LIGHT3, GL.GL_DIFFUSE, light3_diffuse, 0);
        gl.glLightfv(GL.GL_LIGHT3, GL.GL_SPECULAR, light3_specular, 0);

        //yellow light
        float light4_position[] = {.1f, 0, 0, 0};
        float light4_diffuse[] = {.75f, .75f, .05f, 1};
        float light4_specular[] = {.75f, .75f, .05f, 1};
        gl.glLightfv(GL.GL_LIGHT4, GL.GL_POSITION, light4_position, 0);
        gl.glLightfv(GL.GL_LIGHT4, GL.GL_DIFFUSE, light4_diffuse, 0);
        gl.glLightfv(GL.GL_LIGHT4, GL.GL_SPECULAR, light4_specular, 0);

        //material
        float mat_ambient[] = {0, 0, 0, 1};
        float mat_specular[] = {.8f, .8f, .8f, 1};
        float mat_diffuse[] = {.4f, .4f, .4f, 1};
        float mat_shininess[] = {128};
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_AMBIENT, mat_ambient, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_SPECULAR, mat_specular, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_DIFFUSE, mat_diffuse, 0);
        gl.glMaterialfv(GL.GL_FRONT, GL.GL_SHININESS, mat_shininess, 0);

        float bmat_ambient[] = {0, 0, 0, 1};
        float bmat_specular[] = {0, .8f, .8f, 1};
        float bmat_diffuse[] = {0, .4f, .4f, 1};
        float bmat_shininess[] = {128};
        gl.glMaterialfv(GL.GL_BACK, GL.GL_AMBIENT, bmat_ambient, 0);
        gl.glMaterialfv(GL.GL_BACK, GL.GL_SPECULAR, bmat_specular, 0);
        gl.glMaterialfv(GL.GL_BACK, GL.GL_DIFFUSE, bmat_diffuse, 0);
        gl.glMaterialfv(GL.GL_BACK, GL.GL_SHININESS, bmat_shininess, 0);

        float lmodel_ambient[] = {0, 0, 0, 1};
        gl.glLightModelfv(GL.GL_LIGHT_MODEL_AMBIENT, lmodel_ambient, 0);
        gl.glLightModeli(GL.GL_LIGHT_MODEL_TWO_SIDE, 1);

        gl.glEnable(GL.GL_NORMALIZE);
        gl.glEnable(GL.GL_LIGHTING);
        gl.glEnable(GL.GL_LIGHT0);
        gl.glEnable(GL.GL_LIGHT1);
        gl.glEnable(GL.GL_LIGHT2);
        gl.glEnable(GL.GL_LIGHT3);
        gl.glEnable(GL.GL_LIGHT4);

        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LESS);
        gl.glHint(GL.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
        gl.glCullFace(GL.GL_BACK);
        gl.glEnable(GL.GL_CULL_FACE);
        gl.glShadeModel(GL.GL_SMOOTH);

        //create randomly generated object positions
        sceneObjects = new ArrayList<objPosition>();

        //improve to include negative values and possibly alter objPosition
        //to hold an object instead of string (removes switch statement)
        for (int i = 0; i < 10; i++) {
            //change this later for more modular random generation
            float x = (float) (4 * Math.random());
            float z = (float) (4 * Math.random());

            float scale = (float) (4 * Math.random());

            int objectInt = (int) Math.random();

            String object = "tree_aspen";

            switch (objectInt) {
                case 0:
                    sceneObjects.add(new objPosition(tree_aspen, 4, 4));
                    break;
                case 1:
                    sceneObjects.add(new objPosition(tree_conical, 4, 4));
                    break;
                case 2:
                    sceneObjects.add(new objPosition(statue, 4, 4));
                    break;
                case 3:
                    sceneObjects.add(new objPosition(tulip, 4, 4));
                    break;
                case 4:
                    sceneObjects.add(new objPosition(cactus, 4, 4));
                    break;
                case 5:
                    sceneObjects.add(new objPosition(plant, 4, 4));
                    break;
            }

        }
        
        terrain = new TerrainGenerator(terrainDim, terrainSharpness, terrainSize);
        points = terrain.points;
        showTerrain = true;
    }

    public static void main(String[] args) {

        new objReader();
    }

    public void display(GLAutoDrawable drawable) {
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

        gl.glPolygonMode(GL.GL_FRONT_AND_BACK, wireframe ? GL.GL_LINE : GL.GL_FILL);
        gl.glShadeModel(flatshade ? GL.GL_FLAT : GL.GL_SMOOTH);
        if (cullface) {
            gl.glEnable(GL.GL_CULL_FACE);
        } else {
            gl.glDisable(GL.GL_CULL_FACE);
        }

        gl.glLoadIdentity();

        /* this is the transformation of the entire scene */
        gl.glTranslatef(-xpos, -ypos, -zpos);
        gl.glTranslatef(centerx, centery, centerz);
        gl.glRotatef(360.f - roth, 0, 1.0f, 0);
        gl.glRotatef(rotv, 1.0f, 0, 0);
        gl.glTranslatef(-centerx, -centery, -centerz);

        if (showTerrain) {
            gl.glBegin(GL.GL_QUADS);
            for (int i = 0; i < terrain.dim + 1; i++) {
                for (int j = 0; j < terrain.dim + 1; j++) {
                    if (i != terrain.dim && j != terrain.dim) {
                        Vector3f normal = new Vector3f();
                        Vector3f tempVec1 = new Vector3f(points[i + 1][j].x - points[i][j].x, points[i + 1][j].y - points[i][j].y, points[i + 1][j].z - points[i][j].z);
                        Vector3f tempVec2 = new Vector3f(points[i][j + 1].x - points[i][j].x, points[i][j + 1].y - points[i][j].y, points[i][j + 1].z - points[i][j].z);
                        normal.cross(tempVec2, tempVec1);
                        gl.glColor3f(Math.abs(points[i][j].y * 0.3f), Math.abs(points[i][j].y * 1f), Math.abs(points[i][j].y * 0.3f));
                        gl.glNormal3f(normal.x, normal.y, normal.z);
                        gl.glVertex3f(points[i][j].x, points[i][j].y, points[i][j].z);
                        gl.glVertex3f(points[i][j + 1].x, points[i][j + 1].y, points[i][j + 1].z);
                        gl.glVertex3f(points[i + 1][j + 1].x, points[i + 1][j + 1].y, points[i + 1][j + 1].z);
                        gl.glVertex3f(points[i + 1][j].x, points[i + 1][j].y, points[i + 1][j].z);
                    }
                }
            }
            gl.glEnd();
        }
        //simple implementation of random object placement
        for (objPosition o : sceneObjects) {
            gl.glPushMatrix();

            gl.glTranslatef(o.getX(), 0, o.getZ());

            o.getModel().Draw();

            gl.glPopMatrix();
        }

    }

    @Override
    public void mouseClicked(MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
        // TODO Auto-generated method stub
    }

    public void mousePressed(MouseEvent e) {
        mouseX = e.getX();
        mouseY = e.getY();
        mouseButton = e.getButton();
        canvas.display();
    }

    public void mouseReleased(MouseEvent e) {
        mouseButton = MouseEvent.NOBUTTON;
        canvas.display();
    }

    public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        if (mouseButton == MouseEvent.BUTTON3) {
            zpos -= (y - mouseY) * motionSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        } else if (mouseButton == MouseEvent.BUTTON2) {
            xpos -= (x - mouseX) * motionSpeed;
            ypos += (y - mouseY) * motionSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        } else if (mouseButton == MouseEvent.BUTTON1) {
            roth -= (x - mouseX) * rotateSpeed;
            rotv += (y - mouseY) * rotateSpeed;
            mouseX = x;
            mouseY = y;
            canvas.display();
        }
    }

    public void keyPressed(KeyEvent e) {
        switch (e.getKeyCode()) {
            case KeyEvent.VK_ESCAPE:
            case KeyEvent.VK_Q:
                System.exit(0);
                break;
            case KeyEvent.VK_UP:
                terrainSharpness += 0.1f;
                terrain = new TerrainGenerator(terrainDim, terrainSharpness, terrainSize);
                points = terrain.points;
                System.out.println(terrainSharpness);
                break;
            case KeyEvent.VK_DOWN:
                terrainSharpness -= 0.1f;
                terrain = new TerrainGenerator(terrainDim, terrainSharpness, terrainSize);
                points = terrain.points;
                System.out.println(terrainSharpness);
                break;
            case 'w':
            case 'W':
                wireframe = !wireframe;
                break;
            case 'b':
            case 'B':
                cullface = !cullface;
                break;
        }
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void displayChanged(GLAutoDrawable arg0, boolean arg1, boolean arg2) {
        // TODO Auto-generated method stub
    }

    @Override
    public void keyReleased(KeyEvent e) {
        // TODO Auto-generated method stub
    }

    @Override
    public void keyTyped(KeyEvent e) {
        // TODO Auto-generated method stub
    }
}
